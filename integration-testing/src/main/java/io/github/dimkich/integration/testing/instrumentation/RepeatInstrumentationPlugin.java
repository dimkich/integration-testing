package io.github.dimkich.integration.testing.instrumentation;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.HashSet;
import java.util.Set;

import static org.springframework.core.annotation.AnnotatedElementUtils.findMergedAnnotation;

/**
 * Instrumentation plugin that retransforms already loaded classes whose names start
 * with any prefix declared in {@link RepeatInstrumentation} on the test class.
 */
public class RepeatInstrumentationPlugin implements InstrumentationPlugin {

    @Override
    public boolean isApplicable(Class<?> testClass) {
        return testClass.isAnnotationPresent(RepeatInstrumentation.class);
    }

    @Override
    public void afterInstall(Class<?> testClass, Instrumentation instrumentation) throws UnmodifiableClassException {
        RepeatInstrumentation ri = findMergedAnnotation(testClass, RepeatInstrumentation.class);
        if (ri == null) {
            return;
        }
        Set<Class<?>> classes = new HashSet<>();
        for (Class<?> clazz : instrumentation.getAllLoadedClasses()) {
            if (!instrumentation.isModifiableClass(clazz)) {
                continue;
            }
            for (String name : ri.value()) {
                if (clazz.getName().startsWith(name)) {
                    classes.add(clazz);
                }
            }
        }
        if (!classes.isEmpty()) {
            instrumentation.retransformClasses(classes.toArray(new Class[]{}));
        }
    }
}
