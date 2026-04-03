package com.datalinkx.esdriver;


import com.datalinkx.driver.dsdriver.base.writer.AbstractWriter;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@SuperBuilder(toBuilder = true)
public class EsWriter extends AbstractWriter {
    private String address;
    private String username;
    private String password;
    private String index;
    private String type;
    private Long bulkAction;
    private Long timeout;
    private List<Map<String, Object>> idColumn;
    private List<EsColumn> column;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static final class EsColumn {
        private String name;
        private String type;
    }
}
