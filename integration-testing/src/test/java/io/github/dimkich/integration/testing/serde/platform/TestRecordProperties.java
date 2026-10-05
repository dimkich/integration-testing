package io.github.dimkich.integration.testing.serde.platform;

import io.github.dimkich.integration.testing.serde.StandardSerdeProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TestRecordProperties extends StandardSerdeProperties {
    private String keyPrefix;
    private String headerName;
}
