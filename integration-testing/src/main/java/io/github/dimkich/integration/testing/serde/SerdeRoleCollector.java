package io.github.dimkich.integration.testing.serde;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects the roles a serde configuration level reports about itself and validates the
 * combinations of the collected roles.
 *
 * <p>Configuration interfaces report the facts about themselves from
 * {@link TestSerdeProperties#reportRoles(SerdeRoleCollector)}: a property name, the role it
 * plays and, when needed, a condition or a description. The wording of the validation messages
 * belongs to the collector, so a role interface never mentions the properties of another role.
 * A class combining several role interfaces lists them explicitly and calls the corresponding
 * {@code super} methods. The collector is single-use: every configuration level is validated
 * through its own instance.
 *
 * @see TestSerdeProperties#reportRoles(SerdeRoleCollector)
 */
public class SerdeRoleCollector {

    private enum SourceType {
        NON_PARAMETRIC,
        PARAMETRIC
    }

    private record SourceDesc(SourceType type, String propertyName, String roleDescription,
                              String condition) {
    }

    private record DecoratorDesc(String propertyName, String description) {
    }

    private SourceDesc source;
    private final List<String> optionNames = new ArrayList<>();
    private DecoratorDesc decorator;

    /**
     * Registers a source that cannot be parameterized: an existing bean or a fully qualified
     * class name. Options are not applied to such a source.
     *
     * @param propertyName    the configuration field holding the source
     * @param roleDescription the role of the source for "both sources are set" errors
     * @param condition       the condition of the source for "options have no effect" errors
     * @throws IllegalArgumentException if another source was already registered
     */
    public void addNonParametricSource(String propertyName, String roleDescription, String condition) {
        registerSource(new SourceDesc(SourceType.NON_PARAMETRIC, propertyName, roleDescription, condition));
    }

    /**
     * Registers a source that accepts options: a named provider.
     *
     * @param propertyName    the configuration field holding the source
     * @param roleDescription the role of the source for "both sources are set" errors
     * @throws IllegalArgumentException if another source was already registered
     */
    public void addParametricSource(String propertyName, String roleDescription) {
        registerSource(new SourceDesc(SourceType.PARAMETRIC, propertyName, roleDescription, null));
    }

    /**
     * Registers an option applied to the source.
     *
     * @param propertyName the configuration field (or a combined label) holding the option
     */
    public void addOption(String propertyName) {
        optionNames.add(propertyName);
    }

    /**
     * Registers a decorator that requires a source to wrap.
     *
     * @param propertyName the configuration field holding the decorator
     * @param description  what the decorator wraps
     */
    public void addDecorator(String propertyName, String description) {
        decorator = new DecoratorDesc(propertyName, description);
    }

    /**
     * Validates the collected roles.
     *
     * @throws IllegalArgumentException if an option has no effect on the registered source or a
     *                                  decorator has no source to wrap
     */
    public void validate() {
        if (!optionNames.isEmpty() && source != null && source.type() == SourceType.NON_PARAMETRIC) {
            throw new IllegalArgumentException(String.format(
                    "Field '%s' has no effect when '%s' %s: these fields are applied only to a provider.",
                    String.join("'/'", optionNames), source.propertyName(), source.condition()));
        }
        if (decorator != null && source == null) {
            throw new IllegalArgumentException(String.format(
                    "Field '%s' has no effect without a source at the same config level: %s, "
                            + "and there is no source to resolve. Put it next to the source it should wrap.",
                    decorator.propertyName(), decorator.description()));
        }
    }

    private void registerSource(SourceDesc newSource) {
        if (source != null) {
            throw new IllegalArgumentException(String.format(
                    "Both '%s' and '%s' are set on the same serde config. "
                            + "They are mutually exclusive: '%s' %s, '%s' %s. Remove one of them.",
                    source.propertyName(), newSource.propertyName(),
                    source.propertyName(), source.roleDescription(),
                    newSource.propertyName(), newSource.roleDescription()));
        }
        source = newSource;
    }
}
