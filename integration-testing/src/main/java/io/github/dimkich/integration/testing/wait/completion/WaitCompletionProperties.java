package io.github.dimkich.integration.testing.wait.completion;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@Data
@ConfigurationProperties(prefix = "integration.testing.wait.completion")
public class WaitCompletionProperties {
    private boolean enabled;
    private boolean kafkaStandardTask;
    private List<Task> tasks;

    @Data
    public static class Task {
        private String startPointCut;
        private String endPointCut;
    }
}
