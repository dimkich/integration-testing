package io.github.dimkich.integration.testing.wait.completion;

import eu.ciechanowiec.sneakyfun.SneakyFunction;
import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Declares how to discover services that expose a notion of "pending tasks"
 * and how to compute the number of such tasks so that tests can wait until
 * all work is finished.
 * <p>
 * The annotation is processed by {@code PendingTasksWaitCompletion} at agent startup.
 * For every declared {@link PendingTasksAwait} the framework:
 * <ul>
 *     <li>Parses {@link #pointcut()} using the Byte Buddy selector parser to find
 *     constructors and/or factory methods that create service instances to be tracked.</li>
 *     <li>Instruments the matched constructors or factory methods so that every
 *     created instance is registered in {@code PendingTasksTracker} together with
 *     a per-pointcut strategy for counting pending tasks.</li>
 *     <li>Uses that strategy later, when tests call the wait-completion API, to
 *     repeatedly compute the total number of pending tasks across all tracked
 *     instances until the number reaches zero.</li>
 * </ul>
 * Typical usage is to put this annotation on an integration test class and
 * describe "which services should be tracked" (via {@link #pointcut()}) and
 * "how to obtain their pending-task count" (via {@link #countPendingTasksMethod()}
 * or {@link #countPendingTasksFunction()}).
 */
@Inherited
@Documented
@IntegrationTesting
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(PendingTasksAwait.List.class)
public @interface PendingTasksAwait {
    /**
     * Selector that defines which classes and factory methods create services
     * whose pending tasks must be tracked.
     * <p>
     * The selector is parsed by the wait-completion Byte Buddy selector parser and
     * translated into a {@code TypeDescription} and (optionally) a {@code MethodDescription}
     * matcher. If the method matcher is missing, all constructors of the matched
     * type are instrumented; otherwise only matching factory methods are instrumented.
     * In both cases, every created instance is handed over to the pending-tasks
     * tracking infrastructure so that its pending tasks can be counted later.
     * <p>
     * For the selector syntax see
     * {@link io.github.dimkich.integration.testing.wait.completion.parser.ByteBuddySelectorParser}.
     */
    String pointcut();

    /**
     * Name of an instance method that can be used to derive the number of
     * pending tasks for a tracked service.
     * <p>
     * If specified, this no-arg method will be invoked reflectively on each
     * tracked instance when the framework needs to obtain the current count
     * of pending tasks. The result is then converted to an {@code int} by the
     * {@link io.github.dimkich.integration.testing.wait.completion.pending.tasks.ReflectionFunction}:
     * <ul>
     *     <li>if the method returns a {@link Number}, its {@link Number#intValue()} is used;</li>
     *     <li>if it returns a {@link java.util.Collection}, its {@link java.util.Collection#size()} is used;</li>
     *     <li>for any other return type an {@link IllegalArgumentException} is thrown.</li>
     * </ul>
     * <p>
     * This is a convenience for existing services that already expose a method
     * from which the pending-task count can be derived. Either
     * {@code countPendingTasksMethod()} or {@link #countPendingTasksFunction()}
     * must be provided.
     */
    String countPendingTasksMethod() default "";

    /**
     * Function strategy that computes the number of pending tasks for a
     * tracked service.
     * <p>
     * The function receives each tracked instance and is expected to return
     * a non-negative {@code int} representing how many tasks are still in
     * progress. This gives full control over how "pending tasks" are defined
     * and can be used when no suitable instance method exists or when more
     * complex logic is required.
     * <p>
     * If a concrete function class is specified here, it is instantiated once
     * per {@link PendingTasksAwait} configuration and then used whenever the
     * wait-completion mechanism needs to re-evaluate the total pending-task
     * count. Either {@link #countPendingTasksFunction()} or
     * {@code countPendingTasksMethod()} must be provided.
     */
    Class<? extends SneakyFunction<Object, Integer, ? extends Exception>> countPendingTasksFunction()
            default NoFunction.class;

    /**
     * Container annotation for repeatable {@link PendingTasksAwait} declarations.
     */
    @Inherited
    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        PendingTasksAwait[] value();
    }

    /**
     * Default function used when no pending-tasks counting strategy is configured.
     * <p>
     * Always throws {@link UnsupportedOperationException} to signal a misconfiguration.
     */
    final class NoFunction implements SneakyFunction<Object, Integer, Exception> {
        @Override
        public Integer apply(Object input) throws Exception {
            throw new UnsupportedOperationException(
                    "No count pending tasks strategy provided. Specify either countPendingTasksMethod()" +
                            " or countPendingTasksFunction()."
            );
        }
    }
}
