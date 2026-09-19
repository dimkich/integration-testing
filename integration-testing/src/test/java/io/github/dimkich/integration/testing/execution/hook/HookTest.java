package io.github.dimkich.integration.testing.execution.hook;

import io.github.dimkich.integration.testing.DynamicTestBuilder;
import io.github.dimkich.integration.testing.instrumentation.RepeatInstrumentation;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.util.stream.Stream;

@OnMethodEnter(pointcut = "t.name('" + HookTest.HOOK + ".HookedService') && m.name('process')",
        action = EnterAction.class)
@OnMethodExit(pointcut = "t.name('" + HookTest.HOOK + ".HookedService') && m.name('process')",
        action = ExitAction.class)
@OnMethodEnter(pointcut = "t.name('" + HookTest.HOOK + ".HookedService') && m.name('conditional')",
        when = "a.arg(0).asString().startsWith('ok')", action = EnterAction.class)
@OnMethodExit(pointcut = "t.name('" + HookTest.HOOK + ".HookedService') && m.name('fail')",
        action = ExitAction.class)
@OnMethodEnter(pointcut = "t.name('" + HookTest.HOOK + ".HookedService') && m.isConstructor()",
        action = EnterAction.class)
@OnMethodEnter(pointcut = "t.name('" + HookTest.HOOK + ".HookedService') && m.name('staticProcess')",
        action = EnterAction.class)
@OnMethodExit(pointcut = "t.name('" + HookTest.HOOK + ".HookedService') && m.name('staticProcess')",
        action = ExitAction.class)
@RepeatInstrumentation({HookTest.HOOK})
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@SpringBootTest(classes = HookTest.Config.class)
public class HookTest {
    static final String HOOK = "io.github.dimkich.integration.testing.execution.hook";

    private final DynamicTestBuilder dynamicTestBuilder;

    @TestFactory
    Stream<DynamicNode> tests() {
        return dynamicTestBuilder.build("execution/hook/hook.xml");
    }

    @Configuration
    @Import(HookInvoker.class)
    static class Config {
    }
}
