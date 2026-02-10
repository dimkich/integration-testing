package io.github.dimkich.integration.testing.expression;

import io.github.dimkich.integration.testing.expression.wrapper.MethodJunction;
import io.github.dimkich.integration.testing.expression.wrapper.TypeJunction;
import io.github.dimkich.integration.testing.util.ByteBuddyUtils;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.asm.Advice;

/**
 * Encapsulates the results of a pointcut expression match and provides the logic
 * to apply transformations to a ByteBuddy {@link AgentBuilder}.
 *
 * <p>This class holds the unique ID of the pointcut and the corresponding junctions
 * for class-level ({@link TypeJunction}) and method-level ({@link MethodJunction}) matching.
 * It serves as a bridge between the expression-based DSL and the ByteBuddy instrumentation engine.</p>
 */
@Getter
@RequiredArgsConstructor
public class PointcutMatch {
    /**
     * The unique identifier assigned to this specific pointcut match.
     */
    private final int pointcutId;

    /**
     * The element matcher junction used to identify target types/classes.
     */
    private final TypeJunction typeJunction;

    /**
     * The element matcher junction used to identify target methods within matched types.
     */
    private final MethodJunction methodJunction;

    /**
     * Applies the instrumentation rule to the provided {@link AgentBuilder}.
     *
     * <p>The transformation process includes:
     * <ol>
     *   <li>Filtering target types using the {@code typeJunction}.</li>
     *   <li>Binding the {@code pointcutId} to the {@link PointcutId} annotation in the advice class.</li>
     *   <li>Applying the specified {@code adviceClass} to methods that match the {@code methodJunction}.</li>
     *   <li>Injecting a parameter writing visitor to handle argument capturing or modification.</li>
     * </ol></p>
     *
     * @param agentBuilder the original ByteBuddy agent builder to be transformed.
     * @param adviceClass  the class containing {@link net.bytebuddy.asm.Advice} templates
     *                     (e.g., {@code @OnMethodEnter} or {@code @OnMethodExit}).
     * @return an extended version of the agent builder with the applied transformation.
     */
    public AgentBuilder.Identified.Extendable apply(AgentBuilder agentBuilder, Class<?> adviceClass) {
        return agentBuilder.type(typeJunction)
                .transform((builder, type, classLoader, module, domain) ->
                        builder.visit(Advice.withCustomMapping()
                                        .bind(PointcutId.class, pointcutId)
                                        .to(adviceClass).on(methodJunction))
                                .visit(ByteBuddyUtils.getParameterWritingVisitorWrapper().apply(type))
                );
    }
}
