package io.github.dimkich.integration.testing.wait.completion;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "integration.testing.wait.completion")
public class WaitCompletionProperties {
    private boolean enabled;
    private boolean kafkaStandardTask;
}
