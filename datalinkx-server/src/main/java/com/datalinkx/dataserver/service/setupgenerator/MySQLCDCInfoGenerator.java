package com.datalinkx.dataserver.service.setupgenerator;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.utils.JsonUtils;
import com.datalinkx.dataserver.bean.domain.DsBean;
import com.datalinkx.driver.dsdriver.setupinfo.MysqlcdcSetupInfo;

import java.util.List;
import java.util.Map;

public class MySQLCDCInfoGenerator implements SetupInfoGenerator<MysqlcdcSetupInfo> {


    @Override
    public MysqlcdcSetupInfo generateSetupInfo(DsBean dsBean) {
        MysqlcdcSetupInfo mysqlcdcSetupInfo = new MysqlcdcSetupInfo();
        mysqlcdcSetupInfo.setServer(dsBean.getHost());
        mysqlcdcSetupInfo.setPort(dsBean.getPort());
        mysqlcdcSetupInfo.setType(MetaConstants.DsType.DS_MYSQLCDC);
        mysqlcdcSetupInfo.setUid(dsBean.getUsername());
        mysqlcdcSetupInfo.setPwd(dsBean.getPassword());
        mysqlcdcSetupInfo.setDatabase(dsBean.getDatabase());

        Map<String, Object> configMap = JsonUtils.json2Map(dsBean.getConfig());
        List<String> listentypeList = (List<String>) configMap.get("listentype");
        mysqlcdcSetupInfo.setCat(String.join(",", listentypeList));

        return mysqlcdcSetupInfo;
    }
}
