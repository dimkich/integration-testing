package io.github.dimkich.integration.testing.expression;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.janino.ClassBodyEvaluator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * A builder class responsible for the on-the-fly compilation of string expressions into Java bytecode.
 *
 * <p>This builder interfaces with a Java compiler (Janino) to create lightweight,
 * high-performance implementations of functional interfaces at runtime. It maps expression
 * variables to specific wrapper types like {@link io.github.dimkich.integration.testing.expression.wrapper.ObjectWrapper}.</p>
 *
 * <h3>Key Responsibilities:</h3>
 * <ul>
 *   <li><b>Type Mapping:</b> Maps string parameter names in expressions to Java classes.</li>
 *   <li><b>Signature Matching:</b> Ensures the expression's return type matches the requested interface.</li>
 *   <li><b>Bytecode Generation:</b> Compiles the raw string into an executable class.</li>
 * </ul>
 *
 * @param <T> The functional interface type to be generated (e.g., {@code SneakyPredicate}).
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ExpressionBuilder<T> {
    private String expression;
    private Class<T> interfaceType;
    private Class<?> returnType;
    private final List<String> paramNames = new ArrayList<>();
    private final List<Class<?>> paramTypes = new ArrayList<>();

    /**
     * Initializes a new builder instance.
     *
     * @param <T> The target interface type.
     * @return a new {@code ExpressionBuilder} instance.
     */
    public static <T> ExpressionBuilder<T> builder() {
        return new ExpressionBuilder<>();
    }

    /**
     * Sets the raw Java expression or code block to be compiled.
     *
     * @param expression The expression string. Supports single lines
     *                   (e.g., {@code "o.asInt() > 0"}) or blocks ({@code "{ return true; }"}).
     * @return the builder instance.
     */
    public ExpressionBuilder<T> expression(String expression) {
        this.expression = expression;
        return this;
    }

    /**
     * Defines the functional interface that the compiled expression must implement.
     *
     * @param interfaceType The class of the interface (e.g., {@code SneakyBiPredicate.class}).
     * @return the builder instance.
     */
    @SuppressWarnings("unchecked")
    public ExpressionBuilder<T> interfaceType(Class<?> interfaceType) {
        this.interfaceType = (Class<T>) interfaceType;
        return this;
    }

    /**
     * Sets the expected return type of the expression's execution.
     *
     * @param returnType The return type class (e.g., {@code boolean.class} or {@code void.class}).
     * @return the builder instance.
     */
    public ExpressionBuilder<T> returnType(Class<?> returnType) {
        this.returnType = returnType;
        return this;
    }

    /**
     * Defines a parameter available within the expression context.
     *
     * @param name The variable name used in the expression (e.g., "o", "t", "m").
     * @param type The wrapper or object type for this variable.
     * @return the builder instance.
     * @example {@code .param("o", ObjectWrapper.class)}
     */
    public ExpressionBuilder<T> param(String name, Class<?> type) {
        this.paramNames.add(name);
        this.paramTypes.add(type);
        return this;
    }

    /**
     * Compiles the expression and returns an instance of the target interface.
     *
     * <p>This method triggers the heavy lifting of parsing and bytecode generation.
     * If the expression contains syntax errors or type mismatches, a runtime exception
     * with detailed compiler feedback is thrown.</p>
     *
     * @return a compiled implementation of type {@code T}.
     * @throws RuntimeException if compilation fails due to syntax or type errors.
     */
    public T build() {
        Objects.requireNonNull(expression, "Expression must not be null");
        Objects.requireNonNull(interfaceType, "Interface type must not be null");

        try {
            String javaExpr = prepare(expression);
            ClassBodyEvaluator cbe = new ClassBodyEvaluator();
            cbe.setParentClassLoader(Thread.currentThread().getContextClassLoader());
            cbe.setImplementedInterfaces(new Class[]{interfaceType});

            Method sam = Arrays.stream(interfaceType.getMethods())
                    .filter(m -> Modifier.isAbstract(m.getModifiers()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Target is not a functional interface"));

            StringBuilder classBody = new StringBuilder();
            classBody.append("public ").append(returnType.getCanonicalName()).append(" ")
                    .append(sam.getName()).append("(");
            for (int i = 0; i < sam.getParameterCount(); i++) {
                classBody.append("Object arg").append(i);
                if (i < sam.getParameterCount() - 1) classBody.append(", ");
            }
            classBody.append(")");
            Class<?>[] exceptionTypes = sam.getExceptionTypes();
            if (exceptionTypes.length > 0) {
                classBody.append(" throws ");
                for (int i = 0; i < exceptionTypes.length; i++) {
                    classBody.append(exceptionTypes[i].getCanonicalName());
                    if (i < exceptionTypes.length - 1) classBody.append(", ");
                }
            }
            classBody.append(" { \n");
            for (int i = 0; i < paramTypes.size(); i++) {
                String typeName = paramTypes.get(i).getCanonicalName();
                String paramName = paramNames.get(i);
                classBody.append("    ").append(typeName).append(" ").append(paramName)
                        .append(" = (").append(typeName).append(") arg").append(i).append(";\n");
            }
            classBody.append("    ");
            if (!sam.getReturnType().equals(void.class)) {
                classBody.append("return ");
            }
            classBody.append(javaExpr).append(";\n");
            classBody.append("}");

            cbe.cook(classBody.toString());

            return createObject(cbe.getClazz());
        } catch (Exception e) {
            log.error("Janino compilation error: [" + expression + "]", e);
            throw new IllegalArgumentException("Janino compilation error: [" + expression + "]. " + e.getMessage(), e);
        }
    }

    private String prepare(String input) {
        StringBuilder sb = new StringBuilder(input.length());
        boolean inString = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);
            if (c == '\\' && i + 1 < input.length()) {
                char next = input.charAt(i + 1);
                if (inString && next == '\'') {
                    sb.append('\'');
                    i++;
                } else {
                    sb.append('\\');
                    sb.append(next);
                    i++;
                }
                continue;
            }
            if (!inString && c == '\'' && i + 1 < input.length() && input.charAt(i + 1) == '\'') {
                sb.append('\'');
                i++;
                continue;
            }
            if (c == '\'') {
                inString = !inString;
                sb.append('"');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private T createObject(Class<?> cls) throws ReflectiveOperationException {
        return (T) cls.getConstructor().newInstance();
    }
}
