package io.github.dimkich.integration.testing.wait.completion;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties that control how long wait-completion strategies block
 * during one stabilization step.
 *
 * <p>All properties use the {@code integration.testing.wait-completion.*} prefix.</p>
 */
@Data
@ConfigurationProperties(prefix = "integration.testing.wait-completion")
public class WaitCompletionProperties {

    /**
     * Maximum wait for one stabilization step, in milliseconds. Applies to
     * {@code @MethodPairAwait}, {@code @MethodCountingAwait} and {@code @QueueLikeAwait}.
     */
    private long timeoutMs = 5000;
}
