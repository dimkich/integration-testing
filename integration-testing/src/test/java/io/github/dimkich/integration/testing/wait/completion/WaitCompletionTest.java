package io.github.dimkich.integration.testing.wait.completion;

import eu.ciechanowiec.sneakyfun.SneakyRunnable;
import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.RepeatInstrumentation;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.wait.completion.future.like.FutureLike5Consumer;
import io.github.dimkich.integration.testing.wait.completion.future.like.FutureLikeTracker;
import io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingTracker;
import io.github.dimkich.integration.testing.wait.completion.method.pair.MethodPairTracker;
import io.github.dimkich.integration.testing.wait.completion.queue.like.QueueLikeFunction6;
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

import static io.github.dimkich.integration.testing.wait.completion.WaitCompletionTest.*;

@FutureLikeAwait(pointcut = "t.name('" + FL + ".FutureLike1') && m.isConstructor()",
        when = "o.isSameClass(" + FL + ".FutureLike1.class)", await = "o.call('await')")
@FutureLikeAwait(pointcut = "t.name('" + FL + ".FutureLike2') && m.isConstructor()", await = "o.call('await')")
@FutureLikeAwait(pointcut = "t.packageStartsWith('" + FL + "') && m.ann('" + FL + ".FutureLike3Ann')", await = "o.call('await')")
@FutureLikeAwait(pointcut = "t.name('" + FL + ".FutureLike4') " +
        "&& t.ann('" + FL + ".FutureLike4Ann') && m.isConstructor()", await = "o.call('await')",
        when = "o.get().getClass().isAnnotationPresent(" + FL + ".FutureLike4Ann.class)")
@FutureLikeAwait(pointcut = "t.inherits('" + FL + ".FutureLike5') && m.name('create') " +
        "&& m.ann('" + FL + ".FutureLike5Ann')", awaitConsumer = FutureLike5Consumer.class)
@FutureLikeAwait(pointcut = "t.name('" + FL + ".FutureLike6') && m.name('create')", await = "o.call('await')")
@MethodCountingAwait(pointcut = "t.name('" + MC + ".MethodCounting1') && m.name('method')",
        when = "o.isSameClass(" + MC + ".MethodCounting1.class)")
@MethodCountingAwait(pointcut = "t.name('" + MC + ".MethodCounting2') && m.name('method')")
@MethodCountingAwait(pointcut = "t.name('" + MC + ".MethodCounting3') && m.name('method')",
        when = "o.isSameClass(" + MC + ".MethodCounting3Child1.class)")
@MethodCountingAwait(pointcut = "t.packageStartsWith('" + MC + "') && m.ann('" + MC + ".MethodCounting4Ann')")
@MethodCountingAwait(pointcut = "t.name('" + MC + ".MethodCounting51') && m.name('method')")
@MethodCountingAwait(pointcut = "t.name('" + MC + ".MethodCounting61') && m.name('method')")
@MethodPairAwait(startPointcut = "t.name('" + MP + ".MethodPair1') && m.name('method')",
        startWhen = "o.isSameClass(" + MP + ".MethodPair1Child1.class)",
        endPointcut = "t.name('" + MP + ".MethodPair1') && m.name('method')",
        endWhen = "o.isSameClass(" + MP + ".MethodPair1Child2.class)")
@MethodPairAwait(startPointcut = "t.name('" + MP + ".MethodPair21') && m.name('method')",
        endPointcut = "t.name('" + MP + ".MethodPair22') && m.name('method')")
@QueueLikeAwait(pointcut = "t.name('" + QL + ".QueueLike1') && m.isConstructor()",
        size = "o.call('taskCount').asInt()")
@QueueLikeAwait(pointcut = "t.name('" + QL + ".QueueLike2') && m.isConstructor()",
        when = "o.isSameClass(" + QL + ".QueueLike2.class)",
        size = "((Byte)o.call('taskCount').get()).intValue()")
@QueueLikeAwait(pointcut = "t.name('" + QL + ".QueueLike3') && m.isConstructor()",
        when = "o.isSameClass(" + QL + ".QueueLike3Child2.class)",
        size = "o.call('taskCount').asList().size()")
@QueueLikeAwait(pointcut = "t.packageStartsWith('" + QL + "') && t.ann('" + QL + ".QueueLike4Ann') && m.isConstructor()",
        when = "o.get().getClass().isAnnotationPresent(" + QL + ".QueueLike4Ann.class)",
        size = "o.call('taskCount').asInt()")
@QueueLikeAwait(pointcut = "t.name('" + QL + ".QueueLike5') && m.name('create')",
        size = "o.call('taskCount').asInt()")
@QueueLikeAwait(pointcut = "t.packageStartsWith('" + QL + "') && m.ann('" + QL + ".QueueLike6Ann')",
        sizeFunction = QueueLikeFunction6.class)
@RepeatInstrumentation({FL, MC, MP, QL})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@SpringBootTest(classes = WaitCompletionTest.Config.class,
        properties = {"integration.testing.wait.completion.enabled=true"})
public class WaitCompletionTest {
    static final String pkg = "io.github.dimkich.integration.testing.wait.completion";
    static final String FL = pkg + ".future.like";
    static final String MC = pkg + ".method.counting";
    static final String MP = pkg + ".method.pair";
    static final String QL = pkg + ".queue.like";
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
