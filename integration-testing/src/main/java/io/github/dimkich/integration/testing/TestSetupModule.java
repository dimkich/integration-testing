package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class TestSetupModule {
    private final List<Class<?>> parentTypes = new ArrayList<>();
    private final List<Pair<Class<?>, String>> subTypesWithName = new ArrayList<>();
    private final List<Class<?>> subTypes = new ArrayList<>();
    private final List<Pair<Class<?>, String>> aliases = new ArrayList<>();
    private final List<com.fasterxml.jackson.databind.Module> jacksonModules = new ArrayList<>();
    private final List<Pair<String, PropertyFilter>> jacksonFilters = new ArrayList<>();
    private HandlerInstantiator handlerInstantiator;

    public TestSetupModule addParentType(Class<?> type) {
        parentTypes.add(type);
        return this;
    }

    public TestSetupModule addSubTypes(Class<?> subType, String name) {
        subTypesWithName.add(Pair.of(subType, name));
        return this;
    }

    public TestSetupModule addSubTypes(Class<?>... classes) {
        subTypes.addAll(Arrays.asList(classes));
        return this;
    }

    public TestSetupModule addSubTypes(JsonSubTypes jsonSubTypes) {
        for (JsonSubTypes.Type type : jsonSubTypes.value()) {
            addSubTypes(type.value(), type.name());
        }
        return this;
    }

    public TestSetupModule addAlias(Class<?> subType, String alias) {
        aliases.add(Pair.of(subType, alias));
        return this;
    }

    public TestSetupModule addJacksonModule(com.fasterxml.jackson.databind.Module jacksonModule) {
        jacksonModules.add(jacksonModule);
        return this;
    }

    public TestSetupModule addJacksonFilter(String id, PropertyFilter filter) {
        jacksonFilters.add(Pair.of(id, filter));
        return this;
    }

    public TestSetupModule setHandlerInstantiator(HandlerInstantiator handlerInstantiator) {
        this.handlerInstantiator = handlerInstantiator;
        return this;
    }
}
