package com.datalinkx.driver.dsdriver.base.deserializer;

import com.datalinkx.common.constants.MetaConstants;
import com.datalinkx.common.utils.ObjectUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.List;

public class EngineBasedSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
        if (value == null) {
            gen.writeNull();
            return;
        }

        String engine = getEngine(value);
        gen.writeStartObject();

        Class<?> clazz = value.getClass();
        writeFields(value, engine, gen, clazz);

        gen.writeEndObject();
    }

    private void writeFields(Object value, String engine, JsonGenerator gen, Class<?> clazz) throws IOException {
        for (Field field : clazz.getDeclaredFields()) {
            EngineField ann = field.getAnnotation(EngineField.class);
            boolean shouldSkip = ann != null && !ObjectUtils.isEmpty(ann.value()) && !includesEngine(ann, engine);

            if (shouldSkip) {
                continue;
            }

            field.setAccessible(true);
            try {
                Object fieldValue = field.get(value);
                if (fieldValue == null) {
                    continue;
                }

                Class<?> fieldType = field.getType();
                if (isSpecialType(fieldType)) {
                    continue;
                }

                String fieldName = getJsonFieldName(field);
                gen.writeFieldName(fieldName);
                writeValue(fieldValue, gen);
            } catch (IllegalAccessException e) {
            }
        }

        Class<?> superClass = clazz.getSuperclass();
        if (superClass != null && superClass != Object.class) {
            writeFields(value, engine, gen, superClass);
        }
    }

    private boolean isSpecialType(Class<?> clazz) {
        String className = clazz.getName();
        return className.startsWith("ch.qos.logback") ||
               className.startsWith("org.slf4j") ||
               className.startsWith("org.apache.log4j") ||
               className.contains("Logger") ||
               clazz.getName().contains("Lock");
    }

    private void writeValue(Object value, JsonGenerator gen) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else if (value instanceof String) {
            gen.writeString((String) value);
        } else if (value instanceof Number) {
            if (value instanceof Integer) {
                gen.writeNumber((Integer) value);
            } else if (value instanceof Long) {
                gen.writeNumber((Long) value);
            } else if (value instanceof Double) {
                gen.writeNumber((Double) value);
            } else if (value instanceof Float) {
                gen.writeNumber((Float) value);
            } else if (value instanceof Short) {
                gen.writeNumber((Short) value);
            } else {
                gen.writeNumber(value.toString());
            }
        } else if (value instanceof Boolean) {
            gen.writeBoolean((Boolean) value);
        } else if (value instanceof List) {
            gen.writeStartArray();
            for (Object item : (List<?>) value) {
                if (item != null && !isSpecialType(item.getClass())) {
                    writeValue(item, gen);
                }
            }
            gen.writeEndArray();
        } else if (value instanceof byte[]) {
            gen.writeBinary((byte[]) value);
        } else if (isSpecialType(value.getClass())) {
            gen.writeNull();
        } else {
            gen.writeObject(value);
        }
    }

    private String getJsonFieldName(Field field) {
        com.fasterxml.jackson.annotation.JsonProperty jsonProp = field.getAnnotation(com.fasterxml.jackson.annotation.JsonProperty.class);
        if (jsonProp != null) {
            return jsonProp.value();
        }
        return field.getName();
    }

    private boolean includesEngine(EngineField ann, String engine) {
        EngineField.Engine[] engines = ann.value();
        if (engines == null || engines.length == 0) {
            return true;
        }
        for (EngineField.Engine e : engines) {
            if (e == EngineField.Engine.FLINKX && "flinkx".equals(engine)) {
                return true;
            }
            if (e == EngineField.Engine.SEATUNNEL && "seatunnel".equals(engine)) {
                return true;
            }
        }
        return false;
    }

    private String getEngine(Object instance) {
        try {
            // 使用 getField() 代替 getDeclaredField()，可以访问继承的字段
            Field engineField = instance.getClass().getField("engine");
            Object engineValue = engineField.get(instance);
            return engineValue != null ? engineValue.toString() : MetaConstants.CommonConstant.FLINKX_ENGINE;
        } catch (NoSuchFieldException e) {
            // 如果 public 字段找不到，尝试遍历继承链查找 declared field
            Class<?> clazz = instance.getClass();
            while (clazz != null && clazz != Object.class) {
                try {
                    Field engineField = clazz.getDeclaredField("engine");
                    engineField.setAccessible(true);
                    Object engineValue = engineField.get(instance);
                    return engineValue != null ? engineValue.toString() : MetaConstants.CommonConstant.FLINKX_ENGINE;
                } catch (NoSuchFieldException | IllegalAccessException eex) {
                    clazz = clazz.getSuperclass();
                }
            }
            return MetaConstants.CommonConstant.FLINKX_ENGINE;
        } catch (Exception e) {
            return MetaConstants.CommonConstant.FLINKX_ENGINE;
        }
    }

    @Override
    public void serializeWithType(Object value, JsonGenerator gen, SerializerProvider serializers, com.fasterxml.jackson.databind.jsontype.TypeSerializer typeSer) throws IOException {
        serialize(value, gen, serializers);
    }
}