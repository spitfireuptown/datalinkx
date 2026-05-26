package com.datalinkx.driver.dsdriver.transformdriver;


import com.datalinkx.driver.dsdriver.base.meta.TransformNodeMeta;
import com.datalinkx.driver.dsdriver.base.transform.TransformNode;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.Map;

public abstract class ITransformDriver {
    public abstract TransformNode transferInfo(Map<String, Object> commonSettings, String meta);
    public abstract String analysisTransferMeta(JsonNode nodeMeta);
    public abstract TransformNodeMeta.ValidateResult verify(JsonNode nodeMeta) throws Exception;
}
