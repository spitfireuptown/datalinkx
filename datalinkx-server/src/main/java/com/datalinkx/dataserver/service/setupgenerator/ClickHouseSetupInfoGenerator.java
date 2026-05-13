package com.datalinkx.dataserver.service.setupgenerator;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.dataserver.bean.domain.DsBean;
import com.datalinkx.driver.dsdriver.setupinfo.JdbcSetupInfo;

public class ClickHouseSetupInfoGenerator implements SetupInfoGenerator<JdbcSetupInfo> {


    @Override
    public JdbcSetupInfo generateSetupInfo(DsBean dsBean) {
        JdbcSetupInfo jdbcSetupInfo = new JdbcSetupInfo();
        jdbcSetupInfo.setServer(dsBean.getHost());
        jdbcSetupInfo.setPort(dsBean.getPort());
        jdbcSetupInfo.setType(MetaConstants.DsType.DS_CLICKHOUSE);
        jdbcSetupInfo.setUid(dsBean.getUsername());
        jdbcSetupInfo.setPwd(dsBean.getPassword());
        jdbcSetupInfo.setDatabase(dsBean.getDatabase());
        return jdbcSetupInfo;
    }
}
