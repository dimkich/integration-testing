package io.github.dimkich.integration.testing.execution.hook;

import io.github.dimkich.integration.testing.expression.ExpressionFactory;
import io.github.dimkich.integration.testing.expression.PointcutMatch;
import io.github.dimkich.integration.testing.expression.PointcutRegistry;
import io.github.dimkich.integration.testing.expression.PointcutSettings;
import io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin;
import net.bytebuddy.agent.builder.AgentBuilder;

import java.util.Collection;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedRepeatableAnnotations;

/**
 * Instrumentation plugin which applies {@link MethodHookAdvice} for every
 * {@link OnMethodEnter} and {@link OnMethodExit} annotation declared on the test class.
 */
public class MethodHookPlugin implements InstrumentationPlugin {

    @Override
    public boolean isApplicable(Class<?> testClass) {
        return !findMergedRepeatableAnnotations(testClass, OnMethodEnter.class).isEmpty() ||
                !findMergedRepeatableAnnotations(testClass, OnMethodExit.class).isEmpty();
    }

    @Override
    public AgentBuilder configureBuilder(Class<?> testClass, AgentBuilder builder) throws Exception {
        Collection<OnMethodEnter> enters = findMergedRepeatableAnnotations(testClass, OnMethodEnter.class);
        for (OnMethodEnter enter : enters) {
            builder = apply(builder, enter.pointcut(), enter.when(), enter.action(), MethodHookAdvice.Enter.class);
        }
        Collection<OnMethodExit> exits = findMergedRepeatableAnnotations(testClass, OnMethodExit.class);
        for (OnMethodExit exit : exits) {
            builder = apply(builder, "(" + exit.pointcut() + ") && m.isMethod()",
                    exit.when(), exit.action(), MethodHookAdvice.Exit.class);
        }
        return builder;
    }

    private AgentBuilder apply(AgentBuilder builder, String pointcut, String when,
                               Class<? extends MethodAction> actionCls, Class<?> adviceCls) throws Exception {
        PointcutMatch match = ExpressionFactory.createPointcutMatch(pointcut);
        PointcutSettings settings = PointcutRegistry.get(match.getPointcutId());
        settings.setWhen(ExpressionFactory.createInvokePredicate(when));
        settings.setAction(actionCls.getDeclaredConstructor().newInstance());
        return match.apply(builder, adviceCls);
    }
}
