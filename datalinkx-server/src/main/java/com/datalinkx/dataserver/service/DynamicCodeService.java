package com.datalinkx.dataserver.service;

import com.datalinkx.dataserver.bean.vo.DynamicCodeVo;

/**
 * 动态编译代码服务
 */
public interface DynamicCodeService {

    /**
     * 校验动态编译源代码并解析输出字段
     *
     * @param sourceCode 源代码
     * @return 校验结果
     */
    DynamicCodeVo.ValidateResult validateAndParse(String sourceCode);
}
