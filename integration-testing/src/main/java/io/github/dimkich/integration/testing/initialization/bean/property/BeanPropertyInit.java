package io.github.dimkich.integration.testing.initialization.bean.property;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.format.common.polymorphic.unwrapped.UnwrappedTypeProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

/**
 * Declarative initialization of Spring bean properties for integration tests.
 * <p>
 * Every property is a field (or a JavaBean property) of a bean; the value is deserialized by
 * the regular test format machinery, so its type is taken from the {@code type} attribute of
 * the value element:
 *
 * <pre>{@code
 * <init type="BeanPropertyInit" bean="candleJobFactory">
 *     <property name="startUpHistoryAmount" type="PeriodDuration">PT0M</property>
 *     <property name="dynamicCandlesFlag" type="Boolean">true</property>
 * </init>
 * }</pre>
 *
 * Several beans may be configured by one declaration, in which case the bean name is
 * specified per property:
 *
 * <pre>{@code
 * <init type="BeanPropertyInit">
 *     <property bean="candleIteratorFactory" name="dynamicCandlesFlag" type="Boolean">true</property>
 *     <property bean="candleJobFactory" name="startUpHistoryAmount" type="PeriodDuration">PT0M</property>
 * </init>
 * }</pre>
 *
 * Unlike {@link io.github.dimkich.integration.testing.initialization.bean.BeanInit}, this
 * initialization keeps the original values of the modified properties and restores them when
 * the declaration goes out of scope: the baselines are captured before the first modification
 * and the property returns to its Spring value when the enclosing test case or container ends.
 *
 * @author dimkich
 * @see io.github.dimkich.integration.testing.initialization.TestInit
 * @see BeanPropertyInitSetup
 */
@Getter
@Setter
@ToString
public class BeanPropertyInit extends TestInit {
    /**
     * The name of the Spring bean whose properties are configured. May be omitted when every
     * {@link PropertyEntry} specifies its own {@link PropertyEntry#getBean() bean}.
     * <p>
     * This is serialized as an XML attribute.
     */
    @JacksonXmlProperty(isAttribute = true)
    private String bean;

    /**
     * Alias for {@link #bean} kept for consistency with other initializations that use
     * {@code name} for the target object. It does not override an explicitly specified
     * {@link #bean}.
     *
     * @param name the name of the Spring bean
     */
    @JacksonXmlProperty(isAttribute = true)
    public void setName(String name) {
        if (this.bean == null) {
            this.bean = name;
        }
    }

    /**
     * Properties to set, in declaration order.
     */
    @JsonProperty("property")
    @JacksonXmlElementWrapper(useWrapping = false)
    private List<PropertyEntry> properties = new ArrayList<>();

    /**
     * A single property of a bean: the bean name (optional), the field or JavaBean property
     * name and the value.
     * <p>
     * Note: the class must stay deserializable through the default constructor. Lombok is
     * configured with {@code anyConstructor.addConstructorProperties=true}, so an all-args
     * constructor gets {@link java.beans.ConstructorProperties} and Jackson starts using it
     * as a property-based creator. Jackson does not support an unwrapped property in a
     * creator, so the all-args constructor must not be generated here.
     */
    @Data
    @NoArgsConstructor
    public static class PropertyEntry {
        /**
         * The name of the Spring bean. Falls back to {@link BeanPropertyInit#getBean()} when
         * absent. This is serialized as an XML attribute.
         */
        @JacksonXmlProperty(isAttribute = true)
        private String bean;

        /**
         * The name of the field or JavaBean property of the bean. This is serialized as an
         * XML attribute.
         */
        @JacksonXmlProperty(isAttribute = true)
        private String name;

        /**
         * The value to set. In XML, it is unwrapped into the body of the {@code <property>}
         * element, and the type is taken from the {@code type} attribute. In JSON, it stays a
         * regular property (see {@code DisablePolymorphicUnwrappedModule}).
         */
        @JsonUnwrapped
        @UnwrappedTypeProperty("type")
        private Object value;
    }
}
