package io.github.dimkich.integration.testing.execution;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "integration.testing.mock")
public class MockInvokeProperties {
    private boolean mockAlwaysCallRealMethods;
    private boolean mockCallRealMethodsOnNoData;
    private boolean mockReturnMockOnNoData;
    private boolean spyCreateData;
}
