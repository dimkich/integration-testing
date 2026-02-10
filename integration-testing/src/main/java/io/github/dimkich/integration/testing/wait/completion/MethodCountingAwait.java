package io.github.dimkich.integration.testing.wait.completion;

import io.github.dimkich.integration.testing.IntegrationTesting;

import java.lang.annotation.*;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Defines an instrumentation strategy that tracks the lifecycle of specific method executions
 * by maintaining an atomic counter of active (in-flight) calls.
 *
 * <p>This strategy is specifically designed for asynchronous or event-driven systems where
 * a "task" is defined by the duration of a method execution. The framework intercepts the
 * entry and exit of matched methods to increment and decrement a global counter.
 * The test execution will block until the counter returns to zero, ensuring all background
 * activities have finished.</p>
 *
 * <h3>Key Characteristics:</h3>
 * <ul>
 *   <li><b>Reference Counting:</b> Unlike future-based tracking, this does not require a handle
 *       to a return object. It simply counts how many threads are currently executing the matched methods.</li>
 *   <li><b>Scope:</b> Ideal for worker loops, message processors (e.g., MINA, QuickFix/J),
 *       or "fire-and-forget" tasks.</li>
 *   <li><b>Strictness:</b> The {@code pointcut} expression <b>must</b> include a method matcher
 *       (e.g., {@code m.name(...)}) to prevent accidental tracking of every method in a class.</li>
 * </ul>
 *
 * <h3>Example Scenarios:</h3>
 *
 * <p><b>1. Tracking a specific message processor (e.g., Apache MINA):</b>
 * <pre>{@code
 * @MethodCountingAwait(
 *     pointcut = "t.name('org.apache.mina.core.polling.AbstractPollingIoProcessor$Processor') && m.name('process')"
 * )
 * }</pre>
 * In this case, the test waits until the MINA {@code Processor.process()} method finishes its current polling cycle.</p>
 *
 * <p><b>2. Conditional tracking based on implementation:</b>
 * <pre>{@code
 * @MethodCountingAwait(
 *     pointcut = "t.name('io.github.dimkich.Worker') && m.name('execute')",
 *     when = "o.isSameClass(io.github.dimkich.HighPriorityWorker.class)"
 * )
 * }</pre>
 * Here, only executions performed by {@code HighPriorityWorker} are counted, even if the pointcut matches the base {@code Worker} class.</p>
 *
 * <p><b>3. Tracking methods with specific annotations:</b>
 * <pre>{@code
 * @MethodCountingAwait(
 *     pointcut = "t.packageStartsWith('io.github.dimkich.tasks') && m.ann('io.github.dimkich.TrackedTask')"
 * )
 * }</pre></p>
 *
 * @see io.github.dimkich.integration.testing.wait.completion.method.counting.MethodCountingWaitCompletion
 * @see io.github.dimkich.integration.testing.expression.ExpressionFactory
 */
@Inherited
@Documented
@IntegrationTesting
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(MethodCountingAwait.List.class)
public @interface MethodCountingAwait {

    /**
     * A DSL expression used to identify the methods whose invocations should be tracked and counted.
     *
     * <p>Available variables in the expression:
     * <ul>
     *   <li>{@code t} (TypeDescriptionWrapper) — matches the class or interface.</li>
     *   <li>{@code m} (MethodDescriptionWrapper) — matches the method name, arguments, or modifiers.</li>
     * </ul>
     *
     * @return a DSL selector string (e.g., {@code "t.name('com.Service') && m.name('run')"}).
     * @throws IllegalArgumentException if the expression does not define a method matcher.
     */
    String pointcut();

    /**
     * A runtime DSL expression evaluated for each method call to decide if it should be counted.
     *
     * <p>This is evaluated inside the ByteBuddy advice on method entry.
     * If it returns {@code false}, the specific call is ignored by the counter.</p>
     *
     * <p>Available variables:
     * <ul>
     *   <li>{@code o} (ObjectWrapper) — the target object instance ('this').</li>
     *   <li>{@code a} (ArgsWrapper) — arguments passed to the method.</li>
     * </ul>
     *
     * @return a DSL condition string (defaults to {@code "true"}).
     */
    String when() default "true";

    /**
     * Container annotation for repeatable {@link MethodCountingAwait} declarations.
     */
    @Inherited
    @Documented
    @Target(TYPE)
    @Retention(RUNTIME)
    @interface List {
        MethodCountingAwait[] value();
    }
}
