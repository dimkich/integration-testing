package io.github.dimkich.integration.testing.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field as belonging to an exclusive group for property inheritance.
 * <p>
 * When merging properties from parent to child (e.g. via {@link PropertyInheritanceMerger}),
 * fields in the same group behave exclusively: if the child has already set any property
 * in the group, other null properties in that group will not inherit from the parent.
 * This prevents mixing parent and child values within a logical choice (e.g. either
 * {@code beanRef} or {@code classRef}, but not both from different levels).
 *
 * @see PropertyInheritanceMerger
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PropertyInheritanceExclusive {
    /**
     * The exclusive group name. Fields sharing the same value form one group.
     *
     * @return the group identifier, default is "default"
     */
    String value() default "default";
}
