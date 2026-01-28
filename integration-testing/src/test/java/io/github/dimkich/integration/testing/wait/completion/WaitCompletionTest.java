package io.github.dimkich.integration.testing.wait.completion;

import eu.ciechanowiec.sneakyfun.SneakyRunnable;
import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.IntegrationTestConfig;
import io.github.dimkich.integration.testing.RepeatInstrumentation;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.wait.completion.future.like.FutureLike5Consumer;
import io.github.dimkich.integration.testing.wait.completion.future.like.FutureLikeTracker;
import io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingTracker;
import io.github.dimkich.integration.testing.wait.completion.method.pair.MethodPairTracker;
import io.github.dimkich.integration.testing.wait.completion.pending.tasks.PendingTasksFunction6;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import static io.github.dimkich.integration.testing.wait.completion.WaitCompletionTest.pkg;

@FutureLikeAwait(pointcut = "class(" + pkg + ".future.like.FutureLike1)", awaitMethod = "await")
@FutureLikeAwait(pointcut = "class(" + pkg + ".future.like.FutureLike2+)", awaitMethod = "await")
@FutureLikeAwait(pointcut = "method(@" + pkg + ".future.like.FutureLike3Ann)", awaitMethod = "await")
@FutureLikeAwait(pointcut = "class(" + pkg + ".future.like.FutureLike4+" + "@" + pkg + ".future.like.FutureLike4Ann)",
        awaitMethod = "await")
@FutureLikeAwait(pointcut = "method(" + pkg + ".future.like.FutureLike5+#create(..)@" + pkg + ".future.like.FutureLike5Ann)",
        awaitConsumer = FutureLike5Consumer.class)
@FutureLikeAwait(pointcut = "method(" + pkg + ".future.like.FutureLike6#create(..))", awaitMethod = "await")
@MethodCountingAwait(pointcut = "method(" + pkg + ".method.counting.MethodCounting1#method(..))")
@MethodCountingAwait(pointcut = "method(" + pkg + ".method.counting.MethodCounting2+#method(..))")
@MethodCountingAwait(pointcut = "method(" + pkg + ".method.counting.MethodCounting3#method(..) " +
        "&& class(" + pkg + ".method.counting.MethodCounting3Child1))")
@MethodCountingAwait(pointcut = "method(@" + pkg + ".method.counting.MethodCounting4Ann)")
@MethodCountingAwait(pointcut = "method(" + pkg + ".method.counting.MethodCounting51#method(..))")
@MethodCountingAwait(pointcut = "method(" + pkg + ".method.counting.MethodCounting61#method())")
@MethodPairAwait(
        startPointcut = "method(" + pkg + ".method.pair.MethodPair1#method(..) && class(" + pkg + ".method.pair.MethodPair1Child1))",
        endPointcut = "method(" + pkg + ".method.pair.MethodPair1#method(..) && class(" + pkg + ".method.pair.MethodPair1Child2))"
)
@MethodPairAwait(startPointcut = "method(" + pkg + ".method.pair.MethodPair21#method(..))",
        endPointcut = "method(" + pkg + ".method.pair.MethodPair22#method(..))")
@PendingTasksAwait(pointcut = "class(" + pkg + ".pending.tasks.PendingTasks1+)", countPendingTasksMethod = "taskCount")
@PendingTasksAwait(pointcut = "class(" + pkg + ".pending.tasks.PendingTasks2)", countPendingTasksMethod = "taskCount")
@PendingTasksAwait(pointcut = "class(" + pkg + ".pending.tasks.PendingTasks3Child2)", countPendingTasksMethod = "taskCount")
@PendingTasksAwait(pointcut = "class(@" + pkg + ".pending.tasks.PendingTasks4Ann)", countPendingTasksMethod = "taskCount")
@PendingTasksAwait(pointcut = "method(" + pkg + ".pending.tasks.PendingTasks5#create(..))", countPendingTasksMethod = "taskCount")
@PendingTasksAwait(pointcut = "method(@" + pkg + ".pending.tasks.PendingTasks6Ann)",
        countPendingTasksFunction = PendingTasksFunction6.class)
@RepeatInstrumentation({pkg + ".future.like", pkg + ".method.counting", pkg + ".method.pair", pkg + ".pending.tasks"})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@SpringBootTest(classes = {IntegrationTestConfig.class, WaitCompletionTest.Config.class},
        properties = {"integration.testing.wait.completion.enabled=true"})
public class WaitCompletionTest {
    static final String pkg = "io.github.dimkich.integration.testing.wait.completion";
    private final DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() throws Exception {
        return dynamicTestBuilder.build("wait.completion/waitCompletion.xml");
    }

    @Configuration
    @Import(Reflection.class)
    static class Config {
        @Bean
        TestSetupModule module() {
            return new TestSetupModule().addSubTypes(Method.class).addSubTypes(pkg);
        }
    }

    @RequiredArgsConstructor
    @Component(value = "reflection")
    public static class Reflection {
        private final WaitCompletionList waitCompletionList;

        public List<String> create(List<Class<?>> classes) throws Exception {
            return invoke(() -> {
                for (Class<?> cls : classes) {
                    cls.getConstructor().newInstance();
                }
            });
        }

        public List<String> invoke(List<Method> methods) throws Exception {
            return invoke(() -> {
                for (Method method : methods) {
                    Object o = method.getCls().getConstructor().newInstance();
                    try {
                        method.getCls().getMethod(method.getMethod()).invoke(o);
                    } catch (NoSuchMethodException e) {
                        java.lang.reflect.Method m = method.getCls().getDeclaredMethod(method.getMethod());
                        m.setAccessible(true);
                        m.invoke(o);
                    }
                }
            });
        }

        private List<String> invoke(SneakyRunnable<Exception> runnable) throws Exception {
            MethodCalls.reset();
            waitCompletionList.start();
            runnable.run();
            waitCompletionList.waitCompletion();
            List<String> result = MethodCalls.getMethods();
            result.sort(Comparator.naturalOrder());
            result.add("FutureLikeTracker.activeTasks = " + FutureLikeTracker.getActiveTasks());
            result.add("MethodCountingTracker.activeTasks = " + MethodCountingTracker.getActiveTasks());
            result.add("MethodPairTracker.activeTasks = " + MethodPairTracker.getActiveTasks());
            System.gc();
            return result;
        }
    }

    @Data
    public static class Method {
        private final Class<?> cls;
        private final String method;
    }
}
