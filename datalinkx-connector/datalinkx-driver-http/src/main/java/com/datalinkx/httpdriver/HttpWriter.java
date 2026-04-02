package com.datalinkx.httpdriver;

import com.datalinkx.driver.dsdriver.base.writer.AbstractWriter;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class HttpWriter extends AbstractWriter {
    private String url;
    private Map<String, Object> headers = new HashMap<>();
}
