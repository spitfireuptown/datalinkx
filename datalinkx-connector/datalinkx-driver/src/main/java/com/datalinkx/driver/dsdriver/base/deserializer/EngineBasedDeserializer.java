package com.datalinkx.driver.dsdriver.base.deserializer;

import com.datalinkx.common.constants.MetaConstants;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.lang.reflect.Field;

public class EngineBasedDeserializer extends JsonDeserializer<Object> {

    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    public Object deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException {
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);
        
        // 获取目标类型
        Class<?> clazz = deserializationContext.getContextualType().getRawClass();
        
        // 先反序列化所有字段
        Object instance = mapper.treeToValue(node, clazz);
        
        String engine = node.has("engine") ? node.get("engine").asText() : MetaConstants.CommonConstant.FLINKX_ENGINE;
        clearDeserializerFields(instance, clazz, engine);

        return instance;
    }

    private void clearDeserializerFields(Object instance, Class<?> clazz, String engine) {
        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {
            EngineField ann = field.getAnnotation(EngineField.class);
            if (ann != null) {
                boolean includes = includesEngine(ann, engine);
                if (!includes) {
                    try {
                        field.setAccessible(true);
                        field.set(instance, null);
                    } catch (IllegalAccessException e) {
                    }
                }
            }
        }
        
        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            clearDeserializerFields(instance, superClass, engine);
        }
    }

    private boolean includesEngine(EngineField ann, String engine) {
        for (EngineField.Engine e : ann.value()) {
            if (e == EngineField.Engine.FLINKX && MetaConstants.CommonConstant.FLINKX_ENGINE.equals(engine)) {
                return true;
            }
            if (e == EngineField.Engine.SEATUNNEL && MetaConstants.CommonConstant.SEATUNNEL_ENGINE.equals(engine)) {
                return true;
            }
        }
        return false;
    }
}
