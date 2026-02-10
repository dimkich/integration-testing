package io.github.dimkich.integration.testing.expression.wrapper;

import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.function.BiPredicate;

/**
 * A ByteBuddy {@link ElementMatcher} implementation that evaluates types (classes)
 * using compiled string expressions.
 *
 * <p>Used during the initial phase of instrumentation to determine if a class should
 * be transformed. Since no specific method context is available at this stage,
 * it passes a "neutral" (always-true) {@link MethodDescriptionWrapper} to the
 * underlying bi-predicate.</p>
 */
@RequiredArgsConstructor
public class TypeJunction extends ElementMatcher.Junction.AbstractBase<TypeDescription> {
    /**
     * A neutral method wrapper used when matching types without a method context.
     */
    private final static MethodDescriptionWrapper alwaysTrueMethod = new MethodDescriptionWrapper(null);

    /**
     * The compiled expression predicate used to match the type.
     */
    private final BiPredicate<TypeDescriptionWrapper, MethodDescriptionWrapper> predicate;

    /**
     * Evaluates whether the target type matches the expression criteria.
     *
     * @param target the type description provided by ByteBuddy.
     * @return {@code true} if the expression predicate matches the type.
     */
    @Override
    public boolean matches(TypeDescription target) {
        return predicate.test(new TypeDescriptionWrapper(target), alwaysTrueMethod);
    }
}
