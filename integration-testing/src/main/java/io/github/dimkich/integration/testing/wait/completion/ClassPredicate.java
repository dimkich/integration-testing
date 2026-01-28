package io.github.dimkich.integration.testing.wait.completion;

import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiPredicate;

/**
 * {@link BiPredicate} implementation that evaluates whether a given {@link Class}
 * matches a previously registered Byte Buddy type pointcut.
 * <p>
 * Pointcuts are registered via {@link #put(String, ElementMatcher.Junction)} and
 * looked up by name when {@link #test(String, Class)} is invoked.
 * To remove all registered pointcuts use {@link #clear()}.
 */
public class ClassPredicate implements BiPredicate<String, Class<?>> {
    private static final Map<Class<?>, TypeDescription> types = new ConcurrentHashMap<>();
    private static final Map<String, ElementMatcher.Junction<TypeDescription>> typePointcuts = new ConcurrentHashMap<>();

    /**
     * Registers a new type pointcut under the given name.
     *
     * @param pointcut the logical name of the pointcut
     * @param matcher  the Byte Buddy type matcher associated with the pointcut
     */
    public static void put(String pointcut, ElementMatcher.Junction<TypeDescription> matcher) {
        typePointcuts.put(pointcut, matcher);
    }

    /**
     * Removes all registered type pointcuts.
     * <p>
     * This also implicitly clears any cached {@link TypeDescription} instances
     * that are no longer referenced by matchers.
     */
    public static void clear() {
        typePointcuts.clear();
    }

    /**
     * Evaluates whether the supplied class matches the pointcut identified by the given name.
     *
     * @param pointcut the name of a previously registered pointcut
     * @param cls      the class to be tested against the pointcut
     * @return {@code true} if the class matches the pointcut, {@code false} otherwise
     * @throws IllegalArgumentException if no pointcut with the given name is registered
     */
    @Override
    public boolean test(String pointcut, Class<?> cls) {
        ElementMatcher.Junction<TypeDescription> matcher = typePointcuts.get(pointcut);
        if (matcher == null) {
            throw new IllegalArgumentException(String.format("Pointcut '%s' not found", pointcut));
        }
        return matcher.matches(types.computeIfAbsent(cls, TypeDescription.ForLoadedType::of));
    }
}
