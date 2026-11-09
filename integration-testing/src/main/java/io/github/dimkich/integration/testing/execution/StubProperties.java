package io.github.dimkich.integration.testing.execution;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties that control how integration-testing stubs behave.
 * <p>
 * All properties use the {@code integration.testing.stub.*} prefix.
 */
@Data
@ConfigurationProperties(prefix = "integration.testing.stub")
public class StubProperties {

    /**
     * If {@code true}, stubs will always delegate to real methods
     * (equivalent to Mockito's {@code CALLS_REAL_METHODS} behavior).
     */
    private boolean alwaysCallRealMethods;

    /**
     * If {@code true}, when no stored stub data is found for an invocation,
     * the real method will be called instead.
     */
    private boolean callRealMethodsOnNoData;

    /**
     * If {@code true}, when no stored stub data is found for an invocation,
     * a Mockito mock result will be returned instead of calling the real method.
     */
    private boolean returnMockOnNoData;

    /**
     * If {@code true}, missing stub data will be created automatically when a
     * spy is used.
     */
    private boolean spyCreateData;
}
