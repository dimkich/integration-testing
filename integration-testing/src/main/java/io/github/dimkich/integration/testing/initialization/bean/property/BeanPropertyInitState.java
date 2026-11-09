package io.github.dimkich.integration.testing.initialization.bean.property;

import io.github.dimkich.integration.testing.initialization.TestInitState;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * State of {@link BeanPropertyInit}: the values of bean properties that are actual for the
 * current level of the test hierarchy.
 * <p>
 * A property missing from the state is not configured at this level; when it disappears from
 * the state the setup restores the baseline value captured before the first modification.
 *
 * @author dimkich
 * @see TestInitState
 * @see BeanPropertyInit
 * @see BeanPropertyInitSetup
 */
@Getter
@ToString
@EqualsAndHashCode
public class BeanPropertyInitState implements TestInitState<BeanPropertyInitState> {
    /**
     * Identifies a property of a bean independently of the test level it was declared at.
     *
     * @param beanName     the name of the Spring bean
     * @param propertyName the name of the field or JavaBean property
     */
    public record PropertyKey(String beanName, String propertyName) {
    }

    /**
     * Target values by property, in declaration order. A {@code null} value means that the
     * property must be set to {@code null}.
     */
    private final Map<PropertyKey, Object> properties = new LinkedHashMap<>();

    /**
     * Merges a state of a nested level into this one: more specific declarations override the
     * inherited values of the same property.
     *
     * @param other the state to merge with this state
     * @return this state instance
     */
    @Override
    public BeanPropertyInitState merge(BeanPropertyInitState other) {
        if (other != null) {
            this.properties.putAll(other.properties);
        }
        return this;
    }

    /**
     * Creates a copy of this state, so that a nested level can override values without
     * affecting the state of the parent level.
     *
     * @return a new state instance with the same properties
     */
    @Override
    public BeanPropertyInitState copy() {
        BeanPropertyInitState copy = new BeanPropertyInitState();
        copy.properties.putAll(this.properties);
        return copy;
    }
}
