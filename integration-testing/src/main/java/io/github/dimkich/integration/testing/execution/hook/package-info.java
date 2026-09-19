/**
 * Method hooks for the system under test: annotations that execute custom actions when
 * methods matching a pointcut are entered or exited.
 *
 * <ul>
 *   <li>{@link io.github.dimkich.integration.testing.execution.hook.OnMethodEnter} — action executed
 *       right after entering a method or constructor;</li>
 *   <li>{@link io.github.dimkich.integration.testing.execution.hook.OnMethodExit} — action executed
 *       when a method exits, both normally and with an exception;</li>
 *   <li>{@link io.github.dimkich.integration.testing.execution.hook.MethodAction} — the action
 *       contract implemented by user code.</li>
 * </ul>
 *
 * <p>Hooks are installed by {@code MethodHookPlugin} through the
 * {@link io.github.dimkich.integration.testing.instrumentation.InstrumentationPlugin} SPI.
 * A pointcut may use the variables {@code t} (TypeDescriptionWrapper) and
 * {@code m} (MethodDescriptionWrapper), and the {@code when} condition is evaluated
 * dynamically before the action runs.</p>
 */
package io.github.dimkich.integration.testing.execution.hook;
