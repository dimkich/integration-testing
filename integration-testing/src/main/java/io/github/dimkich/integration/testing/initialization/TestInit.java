package io.github.dimkich.integration.testing.initialization;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.Test;
import lombok.Getter;
import lombok.Setter;

/**
 * Abstract base class for test initialization configurations.
 * This class provides common properties for configuring how tests are initialized,
 * including the test type to apply the initialization to and a reference to the parent test.
 */
@Getter
@Setter
public abstract class TestInit {
    /**
     * The type of test that this initialization configuration applies to.
     * This is serialized as an XML attribute when using Jackson XML serialization.
     */
    @JacksonXmlProperty(isAttribute = true)
    private Test.Type applyTo;

    /**
     * Reference to the parent test that contains this initialization configuration.
     * This is marked as a back reference to prevent circular serialization issues
     * when using Jackson JSON serialization.
     */
    @JsonBackReference
    private Test test;
}
