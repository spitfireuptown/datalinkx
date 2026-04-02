package com.datalinkx.driver.dsdriver.transformdriver;

import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Constructor;

/**
 * @author: uptown
 * @date: 2024/11/17 17:56
 */
@Slf4j
public final class ITransformFactory {

    private ITransformFactory() {

    }

    private static final String PACKAGE_PREFIX = "com.datalinkx.driver.dsdriver.transformdriver.";


    public static ITransformDriver getComputeDriver(String type) throws Exception {
        String driverClassName = getDriverClass(type);
        Class<?> driverClass = Class.forName(driverClassName);
        Constructor<?> constructor = driverClass.getDeclaredConstructor();
        return (ITransformDriver) constructor.newInstance();
    }

    private static String getDriverClass(String type) {
        return PACKAGE_PREFIX + type.toUpperCase() + "TransformDriver";
    }
}
