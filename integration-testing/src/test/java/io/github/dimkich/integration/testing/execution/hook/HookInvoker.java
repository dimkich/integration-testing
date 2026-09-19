package io.github.dimkich.integration.testing.execution.hook;

import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.function.Supplier;

@Component("hookInvoker")
public class HookInvoker {
    public List<String> create(String name) {
        return run(() -> {
            new HookedService(name);
            return null;
        });
    }

    public List<String> process(String value) {
        HookedService service = new HookedService("svc");
        return run(() -> service.process(value));
    }

    public List<String> conditional(String value) {
        HookedService service = new HookedService("svc");
        return run(() -> service.conditional(value));
    }

    public List<String> fail(String value) {
        HookedService service = new HookedService("svc");
        HookEvents.reset();
        String result;
        try {
            result = service.fail(value);
        } catch (Exception e) {
            result = "thrown: " + e.getMessage();
        }
        return finish(result);
    }

    public List<String> staticProcess(String value) {
        return run(() -> HookedService.staticProcess(value));
    }

    private List<String> run(Supplier<String> action) {
        HookEvents.reset();
        return finish(action.get());
    }

    private List<String> finish(String result) {
        List<String> events = HookEvents.get();
        events.sort(Comparator.naturalOrder());
        events.add("result = " + result);
        return events;
    }
}
