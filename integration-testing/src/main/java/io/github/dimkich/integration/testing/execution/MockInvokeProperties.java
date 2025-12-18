package io.github.dimkich.integration.testing.execution;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties that control how integration-testing mocks behave.
 * <p>
 * All properties use the {@code integration.testing.mock.*} prefix.
 */
@Data
@ConfigurationProperties(prefix = "integration.testing.mock")
public class MockInvokeProperties {

    /**
     * If {@code true}, mocks will always delegate to real methods
     * (equivalent to Mockito's {@code CALLS_REAL_METHODS} behavior).
     */
    private boolean mockAlwaysCallRealMethods;

    /**
     * If {@code true}, when no stored mock data is found for an invocation,
     * the real method will be called instead.
     */
    private boolean mockCallRealMethodsOnNoData;

    /**
     * If {@code true}, when no stored mock data is found for an invocation,
     * a mock result will be returned instead of calling the real method.
     */
    private boolean mockReturnMockOnNoData;

    /**
     * If {@code true}, missing mock data will be created automatically when a
     * spy is used.
     */
    private boolean spyCreateData;
}
