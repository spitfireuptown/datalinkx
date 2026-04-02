package com.datalinkx.driver.dsdriver.base.transform;

import com.datalinkx.driver.dsdriver.transformdriver.TransformNode;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

/**
 * @author: uptown
 * @date: 2024/10/27 18:00
 */
@SuperBuilder(toBuilder = true)
@EqualsAndHashCode(callSuper = true)
@Data
public class SQLNode extends TransformNode {
    private String query;
}
