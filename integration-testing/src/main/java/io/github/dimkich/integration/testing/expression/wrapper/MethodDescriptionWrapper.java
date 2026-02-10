package io.github.dimkich.integration.testing.expression.wrapper;

import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.description.method.ParameterList;

/**
 * A fluent wrapper around ByteBuddy's {@link MethodDescription}, designed for use
 * within pointcut expressions.
 *
 * <p>This class provides a high-level API to query method properties such as names,
 * parameters, return types, and modifiers. In the context of the expression DSL,
 * it is typically accessed via the {@code m} variable.</p>
 *
 * <p><b>Example expressions:</b>
 * <ul>
 *   <li>{@code m.name('get') && m.args(1)} — matches any 'get' method with one argument.</li>
 *   <li>{@code m.name('add') && m.args('java.lang.Object')} — matches 'add' with a specific parameter type.</li>
 *   <li>{@code m.ann('java.lang.SafeVarargs')} — matches methods annotated with @SafeVarargs.</li>
 *   <li>{@code m.returns('java.lang.Object') && m.isPublic()} — matches public methods returning Object.</li>
 * </ul></p>
 *
 * <p><b>Note:</b> If the underlying delegate is {@code null}, most methods return
 * {@code true} to act as a neutral element in logical junctions (AND/OR).</p>
 */
@RequiredArgsConstructor
public class MethodDescriptionWrapper {
    /**
     * The actual ByteBuddy method description being wrapped.
     */
    private final MethodDescription delegate;

    /**
     * Checks if the method name matches the given string.
     *
     * @param n the expected method name.
     * @return {@code true} if names match or delegate is null.
     */
    public boolean name(String n) {
        return delegate == null || delegate.getName().equals(n);
    }

    /**
     * @return {@code true} if the target is a constructor.
     */
    public boolean isConstructor() {
        return delegate == null || delegate.isConstructor();
    }

    /**
     * @return {@code true} if the target is a standard method.
     */
    public boolean isMethod() {
        return delegate == null || delegate.isMethod();
    }

    /**
     * @return {@code true} if the target is a static type initializer.
     */
    public boolean isTypeInitializer() {
        return delegate == null || delegate.isTypeInitializer();
    }

    /**
     * Checks if the method has exactly the specified number of parameters.
     *
     * @param count expected parameter count.
     * @return {@code true} if the count matches.
     */
    public boolean args(int count) {
        return delegate == null || delegate.getParameters().size() == count;
    }

    /**
     * Checks if the method parameter types match the provided Fully Qualified Class Names (FQCN).
     *
     * @param types array of type names (e.g., {@code "java.lang.String", "int"}).
     * @return {@code true} if the parameter list matches the types exactly in order.
     * @example {@code m.args('java.lang.String', 'int')}
     */
    public boolean args(String... types) {
        if (delegate == null) {
            return true;
        }
        ParameterList<?> params = delegate.getParameters();
        if (params.size() != types.length) {
            return false;
        }
        for (int i = 0; i < types.length; i++) {
            if (!params.get(i).getType().asErasure().getName().equals(types[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks for the presence of a specific annotation on the method.
     *
     * @param annName FQCN of the annotation.
     * @return {@code true} if the annotation is present.
     */
    public boolean ann(String annName) {
        return delegate == null || delegate.getDeclaredAnnotations().isAnnotationPresent(TypeUtils.resolve(annName));
    }

    /**
     * Checks if the method's return type matches the specified FQCN.
     *
     * @param typeName FQCN of the expected return type.
     * @return {@code true} if the return type matches.
     */
    public boolean returns(String typeName) {
        return delegate == null || delegate.getReturnType().asErasure().getName().equals(typeName);
    }

    /**
     * @return {@code true} if the method has the {@code static} modifier.
     */
    public boolean isStatic() {
        return delegate == null || delegate.isStatic();
    }

    /**
     * @return {@code true} if the method has the {@code private} modifier.
     */
    public boolean isPrivate() {
        return delegate == null || delegate.isPrivate();
    }

    /**
     * @return {@code true} if the method is package-private (no access modifier).
     */
    public boolean isPackagePrivate() {
        return delegate == null || delegate.isPackagePrivate();
    }

    /**
     * @return {@code true} if the method has the {@code protected} modifier.
     */
    public boolean isProtected() {
        return delegate == null || delegate.isProtected();
    }

    /**
     * @return {@code true} if the method has the {@code public} modifier.
     */
    public boolean isPublic() {
        return delegate == null || delegate.isPublic();
    }

    /**
     * @return {@code true} if the method has the {@code final} modifier.
     */
    public boolean isFinal() {
        return delegate == null || delegate.isFinal();
    }

    /**
     * @return {@code true} if the method is a {@code native} method.
     */
    public boolean isNative() {
        return delegate == null || delegate.isNative();
    }

    /**
     * @return {@code true} if the method has the {@code synchronized} modifier.
     */
    public boolean isSynchronized() {
        return delegate == null || delegate.isSynchronized();
    }
}
