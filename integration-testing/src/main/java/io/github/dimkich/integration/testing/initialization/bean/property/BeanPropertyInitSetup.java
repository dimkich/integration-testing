package io.github.dimkich.integration.testing.initialization.bean.property;

import io.github.dimkich.integration.testing.Test;
import io.github.dimkich.integration.testing.execution.MockAnswer;
import io.github.dimkich.integration.testing.expression.wrapper.TypeUtils;
import io.github.dimkich.integration.testing.initialization.InitSetup;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.test.util.AopTestUtils;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link InitSetup} that sets Spring bean properties for the time of a test
 * and rolls them back when the declaration leaves the scope.
 * <p>
 * The original value of a property is captured before the first modification and kept for the
 * whole life of the bean factory (i.e. of the Spring context). When a property disappears from
 * the state, the captured baseline is written back. That makes the initialization safe for
 * nested test levels:
 *
 * <pre>{@code
 * <init type="BeanPropertyInit" bean="jobFactory">
 *     <property name="startUpHistoryAmount" type="PeriodDuration">PT0M</property>
 * </init>
 * }</pre>
 *
 * <p>The setup runs after the date/time initialization and before storages and {@code BeanInit},
 * so beans read the overridden values when other initializations invoke their methods.
 * <p>A {@code null} value is written as is. A primitive field or setter cannot hold it, so such
 * a declaration fails with an {@link IllegalArgumentException}.
 *
 * @author dimkich
 * @see InitSetup
 * @see BeanPropertyInit
 * @see BeanPropertyInitState
 */
@Slf4j
@RequiredArgsConstructor
public class BeanPropertyInitSetup implements InitSetup<BeanPropertyInit, BeanPropertyInitState> {
    private static final Object NULL_SENTINEL = new Object();

    private final BeanFactory beanFactory;

    /**
     * Baseline values of the properties ever modified by this setup, keyed the same way as in
     * the states. {@link #NULL_SENTINEL} marks an original {@code null}, so that a property
     * that did not exist in the cache can be distinguished from a property whose original
     * value was {@code null}.
     */
    private final Map<BeanPropertyInitState.PropertyKey, Object> originalValues = new ConcurrentHashMap<>();

    /**
     * Returns the class of test initialization configuration that this setup handles.
     *
     * @return the {@link BeanPropertyInit} class
     */
    @Override
    public Class<BeanPropertyInit> getTestCaseInitClass() {
        return BeanPropertyInit.class;
    }

    /**
     * Returns a default empty state for bean property initialization.
     *
     * @return a new empty {@link BeanPropertyInitState} instance
     */
    @Override
    public BeanPropertyInitState defaultState() {
        return new BeanPropertyInitState();
    }

    /**
     * The initialization keeps a state, so that the setup receives the previous and the new
     * states and can restore the values that left the scope.
     *
     * @return {@code true}
     */
    @Override
    public boolean saveState() {
        return true;
    }

    /**
     * Runs after the date/time initialization and before the storage initializations and
     * {@code BeanInit}, so other initializations see the overridden values.
     *
     * @return the order value {@code 500}
     */
    @Override
    public Integer getOrder() {
        return 500;
    }

    /**
     * Converts a {@link BeanPropertyInit} configuration into a {@link BeanPropertyInitState}.
     *
     * @param init the bean property initialization configuration to convert
     * @return a state object with the target values by property
     * @throws IllegalArgumentException if the bean or the property name is missing
     */
    @Override
    public BeanPropertyInitState convert(BeanPropertyInit init) {
        BeanPropertyInitState state = new BeanPropertyInitState();
        for (BeanPropertyInit.PropertyEntry entry : init.getProperties()) {
            String beanName = entry.getBean() != null ? entry.getBean() : init.getBean();
            if (beanName == null || beanName.isBlank()) {
                throw new IllegalArgumentException(
                        "Bean name must be specified in <init> or <property>: " + entry);
            }
            if (entry.getName() == null || entry.getName().isBlank()) {
                throw new IllegalArgumentException("Property name must not be empty: " + entry);
            }
            state.getProperties().put(new BeanPropertyInitState.PropertyKey(beanName, entry.getName()),
                    entry.getValue());
        }
        return state;
    }

    /**
     * Applies the difference between the previous and the new state: writes the values of the
     * new state and restores the baseline values of the properties that left the state.
     *
     * @param oldState the previous state ({@code null} on the first application)
     * @param newState the new state to apply
     * @param test     the test the state is applied to
     */
    @Override
    public void apply(BeanPropertyInitState oldState, BeanPropertyInitState newState, Test test) {
        MockAnswer.enable(() -> {
            Set<BeanPropertyInitState.PropertyKey> allKeys = new LinkedHashSet<>();
            if (oldState != null) {
                allKeys.addAll(oldState.getProperties().keySet());
            }
            if (newState != null) {
                allKeys.addAll(newState.getProperties().keySet());
            }

            for (BeanPropertyInitState.PropertyKey key : allKeys) {
                boolean inOld = oldState != null && oldState.getProperties().containsKey(key);
                boolean inNew = newState != null && newState.getProperties().containsKey(key);
                if (inNew) {
                    Object oldValue = inOld ? oldState.getProperties().get(key) : null;
                    Object targetValue = newState.getProperties().get(key);
                    if (!inOld || !Objects.deepEquals(oldValue, targetValue)) {
                        captureBaseline(key);
                        log.debug("Setting bean '{}' property '{}' = {}", key.beanName(), key.propertyName(),
                                targetValue);
                        setProperty(key, targetValue);
                    }
                } else if (inOld) {
                    Object baseline = getBaseline(key);
                    log.debug("Restoring bean '{}' property '{}' to baseline = {}", key.beanName(),
                            key.propertyName(), baseline);
                    setProperty(key, baseline);
                }
            }
        });
    }

    private void captureBaseline(BeanPropertyInitState.PropertyKey key) {
        if (!originalValues.containsKey(key)) {
            Object currentValue = getProperty(key);
            originalValues.put(key, currentValue != null ? currentValue : NULL_SENTINEL);
        }
    }

    private Object getBaseline(BeanPropertyInitState.PropertyKey key) {
        Object value = originalValues.get(key);
        return value == NULL_SENTINEL ? null : value;
    }

    private Object getTargetBean(String beanName) {
        try {
            return AopTestUtils.getUltimateTargetObject(beanFactory.getBean(beanName));
        } catch (BeansException e) {
            throw new IllegalStateException(
                    String.format("Bean '%s' of BeanPropertyInit not found", beanName), e);
        }
    }

    private Object getProperty(BeanPropertyInitState.PropertyKey key) {
        Object target = getTargetBean(key.beanName());
        Class<?> clazz = target.getClass();
        String property = key.propertyName();

        Method getter = ReflectionUtils.findMethod(clazz, accessorName("get", property));
        if (getter == null) {
            getter = ReflectionUtils.findMethod(clazz, accessorName("is", property));
        }
        if (getter != null && getter.getParameterCount() == 0) {
            ReflectionUtils.makeAccessible(getter);
            return ReflectionUtils.invokeMethod(getter, target);
        }

        Field field = getField(clazz, key);
        return ReflectionUtils.getField(field, target);
    }

    private void setProperty(BeanPropertyInitState.PropertyKey key, Object value) {
        Object target = getTargetBean(key.beanName());
        Class<?> clazz = target.getClass();
        String setterName = accessorName("set", key.propertyName());

        Method incompatibleSetter = null;
        for (Method method : clazz.getMethods()) {
            if (!method.getName().equals(setterName) || method.getParameterCount() != 1) {
                continue;
            }
            Class<?> parameterType = method.getParameterTypes()[0];
            boolean compatible = value == null
                    ? !parameterType.isPrimitive()
                    : TypeUtils.isAssignable(parameterType, value.getClass());
            if (compatible) {
                ReflectionUtils.makeAccessible(method);
                ReflectionUtils.invokeMethod(method, target, value);
                return;
            }
            if (incompatibleSetter == null) {
                incompatibleSetter = method;
            }
        }

        Field field = findField(clazz, key);
        if (field == null) {
            if (incompatibleSetter != null) {
                throw new IllegalArgumentException(String.format(
                        "Value of type %s cannot be set to bean '%s' property '%s' of type %s",
                        value == null ? "null" : value.getClass().getName(), key.beanName(), key.propertyName(),
                        incompatibleSetter.getParameterTypes()[0].getName()));
            }
            throw new IllegalStateException(String.format(
                    "Bean '%s' has no field or setter for property '%s'", key.beanName(), key.propertyName()));
        }
        if (value == null && field.getType().isPrimitive()) {
            throw new IllegalArgumentException(String.format(
                    "Value of type null cannot be set to bean '%s' property '%s' of type %s",
                    key.beanName(), key.propertyName(), field.getType().getName()));
        }
        try {
            ReflectionUtils.setField(field, target, value);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(String.format(
                    "Value of type %s cannot be set to bean '%s' property '%s' of type %s",
                    value == null ? "null" : value.getClass().getName(), key.beanName(), key.propertyName(),
                    field.getType().getName()), e);
        }
    }

    private Field getField(Class<?> clazz, BeanPropertyInitState.PropertyKey key) {
        Field field = findField(clazz, key);
        if (field == null) {
            throw new IllegalStateException(String.format(
                    "Bean '%s' property '%s' has no getter or field, so its original value cannot be captured",
                    key.beanName(), key.propertyName()));
        }
        return field;
    }

    private Field findField(Class<?> clazz, BeanPropertyInitState.PropertyKey key) {
        try {
            return TypeUtils.getField(clazz, key.propertyName());
        } catch (Exception e) {
            log.debug("Field '{}' not found in class {}", key.propertyName(), clazz.getName());
            return null;
        }
    }

    private String accessorName(String prefix, String property) {
        return prefix + Character.toUpperCase(property.charAt(0)) + property.substring(1);
    }
}
