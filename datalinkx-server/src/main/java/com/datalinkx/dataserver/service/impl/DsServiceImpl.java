package com.datalinkx.dataserver.service.impl;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.exception.DatalinkXServerException;
import com.datalinkx.common.result.StatusCode;
import com.datalinkx.common.utils.Base64Utils;
import com.datalinkx.common.utils.ConnectIdUtils;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.dataserver.bean.domain.DsBean;
import com.datalinkx.dataserver.bean.domain.JobBean;
import com.datalinkx.dataserver.bean.dto.JobDto;
import com.datalinkx.dataserver.bean.vo.DsVo;
import com.datalinkx.dataserver.bean.vo.PageVo;
import com.datalinkx.dataserver.controller.form.DsForm;
import com.datalinkx.dataserver.repository.DsRepository;
import com.datalinkx.dataserver.repository.JobRepository;
import com.datalinkx.dataserver.service.DsService;
import com.datalinkx.dataserver.service.setupgenerator.*;
import com.datalinkx.driver.dsdriver.DsDriverFactory;
import com.datalinkx.driver.dsdriver.IDsDriver;
import com.datalinkx.driver.dsdriver.IDsReader;
import com.datalinkx.driver.dsdriver.base.meta.DbTableField;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

import static com.datalinkx.common.utils.IdUtils.genKey;


@Service
@Slf4j
public class DsServiceImpl implements DsService {

	@Autowired
	private DsRepository dsRepository;
	@Autowired
	private JobRepository jobRepository;

	private static final Map<String, SetupInfoGenerator> SETUP_INFO_GENERATORS = new HashMap<>();

	@PostConstruct
	public void init() {
		SETUP_INFO_GENERATORS.put(MetaConstants.DsType.DS_MYSQL, new MySQLSetupInfoGenerator());
		SETUP_INFO_GENERATORS.put(MetaConstants.DsType.DS_ELASTICSEARCH, new EsSetupInfoGenerator());
		SETUP_INFO_GENERATORS.put(MetaConstants.DsType.DS_ORACLE, new OracleSetupInfoGenerator());
		SETUP_INFO_GENERATORS.put(MetaConstants.DsType.DS_REDIS, new RedisSetupInfoGenerator());
		SETUP_INFO_GENERATORS.put(MetaConstants.DsType.DS_HTTP, new HttpSetupInfoGenerator());
		SETUP_INFO_GENERATORS.put(MetaConstants.DsType.DS_KAFKA, new KafkaSetupInfoGenerator());
		SETUP_INFO_GENERATORS.put(MetaConstants.DsType.DS_MYSQLCDC, new MySQLCDCInfoGenerator());
		SETUP_INFO_GENERATORS.put(MetaConstants.DsType.DS_CUSTOM, new CustomSetupInfoGenerator());
		SETUP_INFO_GENERATORS.put(MetaConstants.DsType.DS_CLICKHOUSE, new ClickHouseSetupInfoGenerator());
	}


	/**
	* @Description //数据源创建
	* @Date 2021/1/12 6:13 下午
	* @param form
	* @return String dsId
	**/
	@Transactional(rollbackFor = Exception.class)
	@SneakyThrows
	public String create(DsForm.DsCreateForm form) {
		String dsId = genKey("ds");
		// 1、检测数据源是否重复，重名或者已接入
		DsBean nameCheck = dsRepository.findByName(form.getName());
		if (!ObjectUtils.isEmpty(nameCheck)) {
			throw new DatalinkXServerException(form.getName() + " 数据源名称存在");
		}

		// 2、构造数据源配置信息
		DsBean dsBean = new DsBean();
		dsBean.setDsId(dsId);
		dsBean.setType(form.getType());
		dsBean.setUsername(form.getUsername());
		dsBean.setHost(form.getHost());
		dsBean.setPort(form.getPort());
		dsBean.setName(form.getName());
		dsBean.setDatabase(form.getDatabase());
		dsBean.setConfig(form.getConfig());
		dsBean.setSchema(form.getDatabase());

		if (form.getPassword() != null) {
			dsBean.setPassword(Base64Utils.encodeBase64(form.getPassword().getBytes(StandardCharsets.UTF_8)));
		}

		// 2.2、检查config字符串是否合法
		this.checkConfigFormat(dsBean);
		// 2.3、检查连接情况
		this.checkConnect(dsBean);

		dsRepository.save(dsBean);

		return dsId;
	}

	private void checkConfigFormat(DsBean dsBean) {
		if (!ObjectUtils.isEmpty(dsBean.getConfig())) {
			Map map;
			try {
				 map = JsonUtils.toObject(dsBean.getConfig(), Map.class);
			} catch (Exception e) {
				log.error("dsbean config json format error", e);
				throw new DatalinkXServerException(StatusCode.DS_CONFIG_ERROR, "数据源附加信息转化Json格式异常");
			}
			// HTTP数据源校验url是否合法
			if (MetaConstants.DsType.DS_HTTP.equals(dsBean.getType())) {
				String url = (String) map.get("url");
				if (ObjectUtils.isEmpty(url)) {
					throw new DatalinkXServerException(StatusCode.DS_CONFIG_ERROR, "接口地址未配置");
				}

				if (!url.contains("http") && !url.contains("https")) {
					throw new DatalinkXServerException(StatusCode.DS_CONFIG_ERROR, "接口地址格式不正确，请包含协议");
				}
			}

			// 自定义数据源校验config中是否声明的type
			if (MetaConstants.DsType.DS_CUSTOM.equals(dsBean.getType()) && !map.containsKey("type")) {
				throw new DatalinkXServerException(StatusCode.DS_CONFIG_ERROR, "自定义数据源需要在CONFIG中声明type");
			}
		}
	}


	private void checkConnect(DsBean dsBean) {
		try {
			IDsDriver ignored = DsDriverFactory.getDriver(getConnectId(dsBean));
			ignored.connect(true);
			log.info("connect success");
		} catch (Exception e) {
			log.error("connect error", e);
			throw new DatalinkXServerException(e.getMessage());
		}
	}

	public String getConnectId(DsBean dsBean) {
		SetupInfoGenerator generator = SETUP_INFO_GENERATORS.get(dsBean.getType());
		if (!ObjectUtils.isEmpty(generator)) {
			Object setupInfo = generator.generateSetupInfo(dsBean);
			return ConnectIdUtils.encodeConnectId(JsonUtils.toJson(setupInfo));
		} else {
			Map<String, Object> map = new HashMap<>();
			map.put("type", dsBean.getType());
			return ConnectIdUtils.encodeConnectId(JsonUtils.toJson(map));
		}
	}

    public PageVo<List<DsBean>> dsPage(DsForm.DataSourcePageForm dataSourcePageForm) {
		PageRequest pageRequest = PageRequest.of(dataSourcePageForm.getPageNo() - 1, dataSourcePageForm.getPageSize());
		Page<DsBean> dsBeans = dsRepository.pageQuery(pageRequest, dataSourcePageForm.getName(), dataSourcePageForm.getType());

		List<DsBean> pageDsList = dsBeans.getContent();
		if (MetaConstants.DsType.DS_CUSTOM.equals(dataSourcePageForm.getType())) {
			pageDsList = pageDsList.stream().peek(ds -> {
				Map configMap = JsonUtils.toObject(ds.getConfig(), Map.class);
				ds.setType((String) configMap.get("type"));
            }).collect(Collectors.toList());
		}

		PageVo<List<DsBean>> result = new PageVo<>();
		result.setPageNo(dataSourcePageForm.getPageNo());
		result.setPageSize(dataSourcePageForm.getPageSize());
		result.setData(pageDsList);
		result.setTotalPage(dsBeans.getTotalPages());
		result.setTotal(dsBeans.getTotalElements());
		return result;
	}

	@Transactional(rollbackFor = Exception.class)
	public void del(String dsId) {
		// 校验任务依赖
		List<JobBean> dependJobs = jobRepository.findDependJobId(dsId);
		if (!ObjectUtils.isEmpty(dependJobs)) {
			throw new DatalinkXServerException(StatusCode.DS_HAS_JOB_DEPEND, "数据源存在流转任务依赖");
		}

		dsRepository.deleteByDsId(dsId);
	}

	public DsBean info(String dsId) {
		return dsRepository.findByDsId(dsId)
				.map(dsBean -> {
					if (!ObjectUtils.isEmpty(dsBean.getPassword())) {
						String pwd = null;
						try {
							pwd = new String(Base64Utils.decodeBase64(dsBean.getPassword()));
						} catch (UnsupportedEncodingException e) {
							log.error("ds密码解析失败");
						}
						dsBean.setPassword(pwd);
					}
					return dsBean;
				})
				.orElseThrow(() -> new DatalinkXServerException(StatusCode.DS_NOT_EXISTS, "from ds not exist"));
	}

	public void modify(DsForm.DsCreateForm form) {
		Optional<DsBean> dsCheck = dsRepository.findByDsId(form.getDsId());
		DsBean dsBean = dsCheck.orElseThrow(() -> new DatalinkXServerException(StatusCode.DS_NOT_EXISTS, "ds not exist"));
		dsBean.setUsername(form.getUsername());
		dsBean.setHost(form.getHost());
		dsBean.setPort(form.getPort());
		dsBean.setName(form.getName());
		dsBean.setDatabase(form.getDatabase());
		dsBean.setConfig(form.getConfig());
		dsBean.setSchema(form.getDatabase());
		dsRepository.save(dsBean);
	}

	@SneakyThrows
	public List<String> fetchTables(String dsId) {
		DsBean dsBean = dsRepository.findByDsId(dsId).orElseThrow(() -> new DatalinkXServerException(StatusCode.DS_NOT_EXISTS));
		List<String> tableList = new ArrayList<>();
		try {
			IDsDriver dsDriver = DsDriverFactory.getDriver(getConnectId(dsBean));
			if (dsDriver instanceof IDsReader) {
				IDsReader dsReader = (IDsReader) dsDriver;
				JobDto.JobGraphDto jobGraphDto = this.getJobGraphInfo(dsBean);
				tableList = dsReader.treeTable(jobGraphDto.getDatabase(), jobGraphDto.getSchema());
			}
		} catch (Exception e) {
			log.error("connect error", e);
			throw new DatalinkXServerException(e);
		}
		return tableList;
	}

	public List<DsBean> list() {
		return Optional.ofNullable(dsRepository.findAllByIsDel(0))
		       .map(beans -> beans.stream()
		               .sorted(Comparator.comparing(DsBean::getType))
		               .collect(Collectors.toList()))
		       .orElse(Collections.emptyList());
	}


	public List<DbTableField> fetchFields(String dsId, String tbName) {
		DsBean dsBean = dsRepository.findByDsId(dsId).orElseThrow(() -> new DatalinkXServerException(StatusCode.DS_NOT_EXISTS));
		try {
			IDsDriver dsDriver = DsDriverFactory.getDriver(getConnectId(dsBean));
			if (dsDriver instanceof IDsReader) {
				IDsReader dsReader = (IDsReader) dsDriver;
				JobDto.JobGraphDto jobGraphDto = this.getJobGraphInfo(dsBean);
				return dsReader.getFields(jobGraphDto.getDatabase(), jobGraphDto.getSchema(), tbName);
			}
		} catch (Exception e) {
			log.error("connect error", e);
			throw new DatalinkXServerException(e);
		}
		return new ArrayList<>();
	}


	public JobDto.JobGraphDto getJobGraphInfo(DsBean dsBean) {
		if (!MetaConstants.DsType.DS_CUSTOM.equals(dsBean.getType())) {
			return JobDto.JobGraphDto.builder()
					.schema(dsBean.getSchema())
					.database(dsBean.getDatabase())
					.type(dsBean.getType())
					.build();
		}

		// 自定义插件驱动需要解析config
		Map<String, Object> configMap = JsonUtils.toObject(dsBean.getConfig(), Map.class);
		return JobDto.JobGraphDto.builder()
				.schema(configMap.getOrDefault("schema", configMap.getOrDefault("database", "").toString()).toString())
				.database(configMap.getOrDefault("database", "").toString())
				.type(configMap.get("type").toString())
				.build();
	}

	public String mcpInfo(String name) {
		DsBean dsBean = dsRepository.findByName(name);
		if (ObjectUtils.isEmpty(dsBean)) {
			throw new DatalinkXServerException(StatusCode.DS_NOT_EXISTS, name + "数据源不存在");
		}

		DsVo.MCPDsInfo mcpDsInfo = new DsVo.MCPDsInfo();
		BeanUtils.copyProperties(dsBean, mcpDsInfo);
		return JsonUtils.toJson(mcpDsInfo);
	}
}
