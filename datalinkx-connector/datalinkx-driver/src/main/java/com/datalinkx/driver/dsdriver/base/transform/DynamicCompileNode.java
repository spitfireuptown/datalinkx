package com.datalinkx.driver.dsdriver.base.transform;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

@EqualsAndHashCode(callSuper = true)
@Data
@SuperBuilder(toBuilder = true)
public class DynamicCompileNode extends TransformNode {
    @JsonProperty("compile_language")
    private String compileLanguage;
    @JsonProperty("compile_pattern")
    private String compilePattern;
    @JsonProperty("source_code")
    private String sourceCode;
}
