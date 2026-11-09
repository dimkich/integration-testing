package io.github.dimkich.integration.testing.initialization.stub;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Configuration for initializing and resetting stubs in integration tests.
 * This class allows resetting all stubs in the current test and its parent tests,
 * which is useful for ensuring clean stub state between test runs.
 * <p>
 * The stub initialization can be applied to specific test types using the {@code applyTo}
 * attribute inherited from {@link TestInit}.
 * <p>
 * XML Examples:
 * <pre>{@code
 * <!-- Reset all stubs -->
 * <init type="StubInit" resetAll="true"/>
 *
 * <!-- Reset all stubs with applyTo attribute -->
 * <init type="StubInit" applyTo="TestCase" resetAll="true"/>
 *
 * <!-- Reset all stubs for TestPart only -->
 * <init type="StubInit" applyTo="TestPart" resetAll="true"/>
 * }</pre>
 *
 * @see TestInit
 */
@Getter
@Setter
@ToString
public class StubInit extends TestInit {
    /**
     * Whether to reset all stubs in the current test and all parent tests.
     * When set to {@code true}, all {@link io.github.dimkich.integration.testing.execution.StubCall}
     * objects in the test hierarchy are reset to their initial state.
     * <p>
     * XML Example:
     * <pre>{@code
     * <init type="StubInit" resetAll="true"/>
     * }</pre>
     */
    @JacksonXmlProperty(isAttribute = true)
    private Boolean resetAll;
}
