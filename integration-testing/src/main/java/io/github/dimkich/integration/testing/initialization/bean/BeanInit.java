package io.github.dimkich.integration.testing.initialization.bean;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Represents a bean-based initialization configuration for integration tests.
 * This class extends {@link io.github.dimkich.integration.testing.initialization.TestInit}
 * and provides a mechanism to initialize beans by invoking specific methods on them.
 *
 * <p>Bean initialization is useful when you need to set up test data or configure
 * beans before running tests. Each bean can have one or more methods invoked
 * during the initialization phase.
 *
 * <p>Example XML configuration:
 * <pre>{@code
 * <init type="beanInit" applyTo="TestCase">
 *     <bean name="bean1" method="init"/>
 *     <bean name="bean2" method="setup"/>
 * </init>
 * }</pre>
 *
 * @author dimkich
 * @see io.github.dimkich.integration.testing.initialization.TestInit
 */
@Getter
@Setter
@ToString
public class BeanInit extends TestInit {
    /**
     * List of bean methods to be invoked during test initialization.
     * Each element in this list represents a bean and the method to call on it.
     */
    private List<BeanMethod> bean;

    /**
     * Represents a bean method configuration that specifies which method
     * should be invoked on a particular bean during test initialization.
     *
     * <p>This nested class is used to configure bean initialization by providing
     * the bean name and the method name to invoke. Both properties are serialized
     * as XML attributes when using Jackson XML serialization.
     */
    @Data
    public static class BeanMethod {
        /**
         * The name of the bean to initialize.
         * This is serialized as an XML attribute when using Jackson XML serialization.
         */
        @JacksonXmlProperty(isAttribute = true)
        private String name;

        /**
         * The name of the method to invoke on the specified bean.
         * This is serialized as an XML attribute when using Jackson XML serialization.
         */
        @JacksonXmlProperty(isAttribute = true)
        private String method;
    }
}
