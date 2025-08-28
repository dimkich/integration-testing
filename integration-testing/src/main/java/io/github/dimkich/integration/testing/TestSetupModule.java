package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import io.github.sugarcubes.cloner.CopyAction;
import io.github.sugarcubes.cloner.ReflectionUtils;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.RegexPatternTypeFilter;

import java.lang.reflect.Field;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.Predicate;
import java.util.regex.Pattern;

@Getter
public class TestSetupModule {
    private final List<Class<?>> parentTypes = new ArrayList<>();
    private final List<Pair<Class<?>, String>> subTypesWithName = new ArrayList<>();
    private final List<Class<?>> subTypes = new ArrayList<>();
    private final List<Pair<Class<?>, String>> aliases = new ArrayList<>();
    private final List<com.fasterxml.jackson.databind.Module> jacksonModules = new ArrayList<>();
    private final List<Pair<String, PropertyFilter>> jacksonFilters = new ArrayList<>();
    private final Map<Class<?>, BiPredicate<?, ?>> equalsMap = new HashMap<>();
    private final Map<Field, CopyAction> fieldActions = new LinkedHashMap<>();
    private final Map<Class<?>, CopyAction> typeActions = new LinkedHashMap<>();
    private final Map<Predicate<Class<?>>, CopyAction> predicateTypeActions = new LinkedHashMap<>();
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

    public TestSetupModule addSubTypes(String packageName) {
        return addSubTypes(packageName, Set.of());
    }

    public TestSetupModule addSubTypes(String packageName, Set<Class<?>> exclude) {
        return addSubTypes(packageName, exclude, false);
    }

    @SneakyThrows
    public TestSetupModule addSubTypes(String packageName, Set<Class<?>> exclude, boolean includeInnerClasses) {
        ClassPathScanningCandidateComponentProvider provider = new ClassPathScanningCandidateComponentProvider(false);
        provider.addIncludeFilter(new RegexPatternTypeFilter(Pattern.compile(includeInnerClasses ? ".*" : "[^$]*")));
        for (BeanDefinition bean : provider.findCandidateComponents(packageName)) {
            Class<?> cls = Class.forName(bean.getBeanClassName());
            if (!exclude.contains(cls)) {
                addSubTypes(cls);
            }
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

    public <T> TestSetupModule addEqualsForType(Class<T> type, BiPredicate<? super T, ? super T> equals) {
        equalsMap.put(type, equals);
        return this;
    }

    public TestSetupModule clonerFieldAction(Class<?> type, String field, CopyAction action) {
        clonerFieldAction(ReflectionUtils.getField(type, field), action);
        return this;
    }

    public TestSetupModule clonerFieldAction(Field field, CopyAction action) {
        fieldActions.put(field, action);
        return this;
    }

    public TestSetupModule clonerTypeAction(Class<?> type, CopyAction action) {
        typeActions.put(type, action);
        return this;
    }

    public TestSetupModule clonerTypeAction(Predicate<Class<?>> type, CopyAction action) {
        predicateTypeActions.put(type, action);
        return this;
    }
}
