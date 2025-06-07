package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Getter
public class Module {
    private final List<Class<?>> parentTypes = new ArrayList<>();
    private final List<Pair<Class<?>, String>> subTypesWithName = new ArrayList<>();
    private final List<Class<?>> subTypes = new ArrayList<>();
    private final List<Pair<Class<?>, String>> aliases = new ArrayList<>();
    private final List<com.fasterxml.jackson.databind.Module> jacksonModules = new ArrayList<>();
    private final List<Pair<String, PropertyFilter>> jacksonFilters = new ArrayList<>();

    public Module addParentType(Class<?> type) {
        parentTypes.add(type);
        return this;
    }

    public Module addSubTypes(Class<?> subType, String name) {
        subTypesWithName.add(Pair.of(subType, name));
        return this;
    }

    public Module addSubTypes(Class<?>... classes) {
        subTypes.addAll(Arrays.asList(classes));
        return this;
    }

    public Module addSubTypes(JsonSubTypes jsonSubTypes) {
        for (JsonSubTypes.Type type : jsonSubTypes.value()) {
            addSubTypes(type.value(), type.name());
        }
        return this;
    }

    public Module addAlias(Class<?> subType, String alias) {
        aliases.add(Pair.of(subType, alias));
        return this;
    }

    public Module addJacksonModule(com.fasterxml.jackson.databind.Module jacksonModule) {
        jacksonModules.add(jacksonModule);
        return this;
    }

    public Module addJacksonFilter(String id, PropertyFilter filter) {
        jacksonFilters.add(Pair.of(id, filter));
        return this;
    }
}
