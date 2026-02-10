package io.github.dimkich.integration.testing.expression.wrapper;

import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.function.BiPredicate;

/**
 * A ByteBuddy {@link ElementMatcher} implementation that evaluates methods using
 * compiled string expressions.
 *
 * <p>This junction acts as a bridge between ByteBuddy's instrumentation engine and
 * the expression DSL. It converts a {@link MethodDescription} into a {@link MethodDescriptionWrapper}
 * and tests it against the compiled predicate, which has access to both the method
 * and its declaring type.</p>
 */
@RequiredArgsConstructor
public class MethodJunction extends ElementMatcher.Junction.AbstractBase<MethodDescription> {
    /**
     * The compiled expression predicate used to match the method context.
     */
    private final BiPredicate<TypeDescriptionWrapper, MethodDescriptionWrapper> predicate;

    /**
     * Evaluates whether the target method matches the expression criteria.
     *
     * @param target the method description provided by ByteBuddy.
     * @return {@code true} if the expression predicate matches the method and its declaring type.
     */
    @Override
    public boolean matches(MethodDescription target) {
        return predicate.test(new TypeDescriptionWrapper(target.getDeclaringType().asErasure()),
                new MethodDescriptionWrapper(target));
    }
}
