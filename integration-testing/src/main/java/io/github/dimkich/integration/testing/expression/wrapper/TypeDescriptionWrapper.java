package io.github.dimkich.integration.testing.expression.wrapper;

import lombok.RequiredArgsConstructor;
import net.bytebuddy.description.type.TypeDescription;

/**
 * A fluent wrapper around ByteBuddy's {@link TypeDescription}, providing a high-level API
 * for class-level matching in pointcut expressions.
 *
 * <p>In the expression DSL, this wrapper is typically represented by the {@code t} variable.
 * It allows filtering target types based on names, inheritance, packages, and modifiers.</p>
 *
 * <p>Example expressions:
 * <ul>
 *   <li>{@code t.name('java.util.ArrayList')} - matches exact class.</li>
 *   <li>{@code t.inherits('java.util.List')} - matches any implementation of List.</li>
 *   <li>{@code t.isPublic() && t.packageStartsWith('com.example')} - matches public classes in a specific package.</li>
 * </ul></p>
 */
@RequiredArgsConstructor
public class TypeDescriptionWrapper {

    /**
     * The actual ByteBuddy type description being wrapped.
     */
    public final TypeDescription delegate;

    /**
     * Checks if the type name matches the given string.
     *
     * @param name Fully Qualified Class Name (FQCN), supports arrays (e.g., 'int[]')
     *             and primitives.
     * @return {@code true} if names match.
     */
    public boolean name(String name) {
        return delegate.equals(TypeUtils.resolve(name));
    }

    /**
     * Checks if the simple class name (excluding package) matches the given string.
     *
     * @param name simple class name (e.g., 'ArrayList').
     * @return {@code true} if simple names match.
     */
    public boolean simpleName(String name) {
        return delegate.getSimpleName().equals(name);
    }

    /**
     * Checks if this type inherits from or implements the specified base type.
     *
     * @param baseName FQCN of the expected parent class or interface.
     * @return {@code true} if this type is assignable to the base type.
     */
    public boolean inherits(String baseName) {
        return delegate.isAssignableTo(TypeUtils.resolve(baseName));
    }

    /**
     * Checks for the presence of a specific annotation on the class.
     *
     * @param annName FQCN of the annotation.
     * @return {@code true} if the annotation is present.
     */
    public boolean ann(String annName) {
        return delegate.getDeclaredAnnotations().isAnnotationPresent(TypeUtils.resolve(annName));
    }

    /**
     * Checks if the class belongs to the specified package.
     *
     * @param packageName exact package name (e.g., 'java.util').
     * @return {@code true} if the package matches.
     */
    public boolean inPackage(String packageName) {
        return delegate.getPackage() != null && delegate.getPackage().getActualName().equals(packageName);
    }

    /**
     * Checks if the class package name starts with the given prefix.
     *
     * @param prefix package prefix (e.g., 'java.util').
     * @return {@code true} if the package starts with the prefix.
     */
    public boolean packageStartsWith(String prefix) {
        return delegate.getPackage() != null && delegate.getPackage().getActualName().startsWith(prefix);
    }

    /**
     * @return {@code true} if the type has the {@code public} modifier.
     */
    public boolean isPublic() {
        return delegate.isPublic();
    }

    /**
     * @return {@code true} if the type has the {@code protected} modifier.
     */
    public boolean isProtected() {
        return delegate.isProtected();
    }

    /**
     * @return {@code true} if the type is package-private.
     */
    public boolean isPackagePrivate() {
        return delegate.isPackagePrivate();
    }

    /**
     * @return {@code true} if the type has the {@code private} modifier.
     */
    public boolean isPrivate() {
        return delegate.isPrivate();
    }

    /**
     * @return {@code true} if the type has the {@code abstract} modifier.
     */
    public boolean isAbstract() {
        return delegate.isAbstract();
    }

    /**
     * @return {@code true} if the type is an {@code interface}.
     */
    public boolean isInterface() {
        return delegate.isInterface();
    }

    /**
     * @return {@code true} if the type has the {@code final} modifier.
     */
    public boolean isFinal() {
        return delegate.isFinal();
    }

    /**
     * @return {@code true} if the type is an {@code annotation}.
     */
    public boolean isAnnotation() {
        return delegate.isAnnotation();
    }

    /**
     * @return {@code true} if the type is an {@code enum}.
     */
    public boolean isEnum() {
        return delegate.isEnum();
    }
}
