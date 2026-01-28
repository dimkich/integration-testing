package io.github.dimkich.integration.testing.wait.completion.parser;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.matcher.ElementMatcher;

/**
 * Container for Byte Buddy matchers produced by {@link ByteBuddySelectorParser}.
 * <p>
 * It groups together type and method matchers, as well as an optional type filter
 * that can further restrict which types are considered during instrumentation.
 */
@Getter
@RequiredArgsConstructor
public class ByteBuddySelectorResult {
    private final ElementMatcher.Junction<TypeDescription> typeMatcher;
    private final ElementMatcher.Junction<MethodDescription> methodMatcher;
    private final ElementMatcher.Junction<TypeDescription> typeFilter;

    /**
     * Creates a selector result without an explicit type filter.
     * The {@link #getTypeFilter()} method will fall back to the {@code typeMatcher}.
     *
     * @param typeMatcher   matcher for types to instrument
     * @param methodMatcher matcher for methods to instrument
     */
    public ByteBuddySelectorResult(ElementMatcher.Junction<TypeDescription> typeMatcher,
                                   ElementMatcher.Junction<MethodDescription> methodMatcher) {
        this.typeMatcher = typeMatcher;
        this.methodMatcher = methodMatcher;
        this.typeFilter = null;
    }

    /**
     * Returns the type filter used for instrumentation.
     * If no explicit filter is provided, this falls back to {@code typeMatcher}.
     *
     * @return matcher used to filter instrumented types
     */
    public ElementMatcher.Junction<TypeDescription> getTypeFilter() {
        return typeFilter == null ? typeMatcher : typeFilter;
    }
}
