package io.github.dimkich.integration.testing.initialization.mock;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Configuration for initializing and resetting mocks in integration tests.
 * This class allows resetting all mocks in the current test and its parent tests,
 * which is useful for ensuring clean mock state between test runs.
 * <p>
 * The mock initialization can be applied to specific test types using the {@code applyTo}
 * attribute inherited from {@link TestInit}.
 * <p>
 * XML Examples:
 * <pre>{@code
 * <!-- Reset all mocks -->
 * <init type="mockInit" resetAll="true"/>
 *
 * <!-- Reset all mocks with applyTo attribute -->
 * <init type="mockInit" applyTo="TestCase" resetAll="true"/>
 *
 * <!-- Reset all mocks for TestPart only -->
 * <init type="mockInit" applyTo="TestPart" resetAll="true"/>
 * }</pre>
 *
 * @see TestInit
 */
@Getter
@Setter
@ToString
public class MockInit extends TestInit {
    /**
     * Whether to reset all mocks in the current test and all parent tests.
     * When set to {@code true}, all {@link io.github.dimkich.integration.testing.execution.MockInvoke}
     * objects in the test hierarchy are reset to their initial state.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="mockInit" resetAll="true"/>
     * }</pre>
     */
    @JacksonXmlProperty(isAttribute = true)
    private Boolean resetAll;
}
