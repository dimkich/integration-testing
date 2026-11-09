package io.github.dimkich.integration.testing.collections;

import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.MemberSubstitution;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.matcher.ElementMatcher;
import org.springframework.core.annotation.AnnotatedElementUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.is;
import static net.bytebuddy.matcher.ElementMatchers.nameStartsWith;
import static net.bytebuddy.matcher.ElementMatchers.none;

/**
 * Instrumentation plugin that activates ordered collection replacements for a test class
 * annotated with {@link DeterministicCollections}.
 *
 * <p>For every class whose fully qualified name starts with one of the prefixes declared in
 * {@link DeterministicCollections#value()}, the plugin:</p>
 * <ul>
 *     <li>substitutes order-sensitive JDK collection factories with
 *         {@link OrderedCollections} methods;</li>
 *     <li>optionally rewrites {@code new HashMap(...)} and {@code new HashSet(...)} to
 *         {@code new LinkedHashMap(...)} and {@code new LinkedHashSet(...)} using
 *         {@link HashCollectionRewriter}.</li>
 * </ul>
 */
public class DeterministicCollectionsPlugin implements InstrumentationPlugin {

    private static final Set<String> COLLECTOR_METHODS = Set.of(
            "toSet", "toUnmodifiableSet", "toMap", "toUnmodifiableMap", "groupingBy");

    @Override
    public boolean isApplicable(Class<?> testClass) {
        return AnnotatedElementUtils.findMergedAnnotation(testClass, DeterministicCollections.class) != null;
    }

    @Override
    public AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) {
        DeterministicCollections annotation = AnnotatedElementUtils
                .findMergedAnnotation(testClass, DeterministicCollections.class);
        if (annotation == null) {
            return builder;
        }
        if (annotation.value().length == 0) {
            throw new IllegalStateException(
                    "@DeterministicCollections requires at least one class/package prefix in value()");
        }

        Set<DeterministicCollections.Feature> disabled =
                EnumSet.noneOf(DeterministicCollections.Feature.class);
        disabled.addAll(Arrays.asList(annotation.disabled()));

        MemberSubstitution substitution = createSubstitution(disabled);

        ElementMatcher.Junction<NamedElement> typeMatcher = none();
        for (String prefix : annotation.value()) {
            typeMatcher = typeMatcher.or(nameStartsWith(prefix));
        }

        return builder
                .ignore(nameStartsWith(OrderedCollections.class.getName())
                        .or(nameStartsWith(DeterministicCollectionsPlugin.class.getName()))
                        .or(nameStartsWith(HashCollectionRewriter.class.getName())))
                .type(typeMatcher)
                .transform((builderToTransform, typeDescription, classLoader, module, domain) -> {
                    DynamicType.Builder<?> result = builderToTransform.visit(substitution.on(any()));
                    if (!disabled.contains(DeterministicCollections.Feature.HASH_COLLECTIONS)) {
                        result = result.visit(new HashCollectionRewriter());
                    }
                    return result;
                });
    }

    private static MemberSubstitution createSubstitution(Set<DeterministicCollections.Feature> disabled) {
        MemberSubstitution substitution = MemberSubstitution.relaxed();
        if (!disabled.contains(DeterministicCollections.Feature.MAP_OF)) {
            substitution = substitute(substitution, Map.class, "of", "mapOf");
            substitution = substitute(substitution, Map.class, "ofEntries", "mapOfEntries");
        }
        if (!disabled.contains(DeterministicCollections.Feature.COPY_OF)) {
            substitution = substitute(substitution, Map.class, "copyOf", "mapCopyOf");
            substitution = substitute(substitution, Set.class, "copyOf", "setCopyOf");
        }
        if (!disabled.contains(DeterministicCollections.Feature.SET_OF)) {
            substitution = substitute(substitution, Set.class, "of", "setOf");
        }
        if (!disabled.contains(DeterministicCollections.Feature.COLLECTORS)) {
            substitution = substituteCollectors(substitution);
        }
        return substitution;
    }

    private static MemberSubstitution substitute(MemberSubstitution substitution, Class<?> type,
                                                 String sourceName, String replacementName) {
        MemberSubstitution result = substitution;
        for (Method source : declaredStaticMethods(type, sourceName)) {
            Method replacement = findReplacement(replacementName, source.getParameterTypes());
            if (replacement == null) {
                throw new IllegalStateException("No replacement " + replacementName + " for " + source);
            }
            result = result.method(is(source)).replaceWith(replacement);
        }
        return result;
    }

    private static MemberSubstitution substituteCollectors(MemberSubstitution substitution) {
        MemberSubstitution result = substitution;
        for (Method source : Collectors.class.getDeclaredMethods()) {
            if (!Modifier.isStatic(source.getModifiers()) || !COLLECTOR_METHODS.contains(source.getName())) {
                continue;
            }
            if ("toMap".equals(source.getName()) && source.getParameterCount() == 4) {
                continue;
            }
            Method replacement = findReplacement(source.getName(), source.getParameterTypes());
            if (replacement != null) {
                result = result.method(is(source)).replaceWith(replacement);
            }
        }
        return result;
    }

    private static List<Method> declaredStaticMethods(Class<?> type, String name) {
        return Arrays.stream(type.getDeclaredMethods())
                .filter(method -> Modifier.isStatic(method.getModifiers()))
                .filter(method -> method.getName().equals(name))
                .collect(Collectors.toList());
    }

    private static Method findReplacement(String name, Class<?>[] parameterTypes) {
        try {
            return OrderedCollections.class.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            return null;
        }
    }
}
