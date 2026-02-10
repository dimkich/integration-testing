package io.github.dimkich.integration.testing.expression.wrapper;

import lombok.RequiredArgsConstructor;

/**
 * A safe wrapper for method arguments used within interception expressions.
 *
 * <p>In the expression DSL, this is represented by the {@code a} variable. It provides
 * bounds-checked access to the arguments of the intercepted method, preventing
 * {@link IndexOutOfBoundsException} and handling {@code null} argument arrays gracefully.</p>
 *
 * <p><b>Example expressions:</b>
 * <ul>
 *   <li>{@code a.size() == 2} — checks if the method has exactly two arguments.</li>
 *   <li>{@code a.arg(0).asString().equals('test')} — accesses and validates the first argument.</li>
 *   <li>{@code a.arg(1).isInstance(int.class)} — checks the type of the second argument.</li>
 * </ul></p>
 */
@RequiredArgsConstructor
public class ArgsWrapper {
    /**
     * The raw array of arguments captured during method interception.
     */
    private final Object[] args;

    /**
     * Retrieves an argument at the specified index, wrapped for fluent evaluation.
     *
     * @param index the zero-based index of the argument.
     * @return an {@link ObjectWrapper} for the argument, or {@link ObjectWrapper#NULL}
     * if the index is out of bounds or args are null.
     */
    public ObjectWrapper arg(int index) {
        if (args == null || index < 0 || index >= args.length) {
            return ObjectWrapper.NULL;
        }
        return new ObjectWrapper(args[index]);
    }

    /**
     * Returns the total number of arguments passed to the intercepted method.
     *
     * @return the number of arguments, or 0 if the argument array is null.
     */
    public int size() {
        return args == null ? 0 : args.length;
    }
}
