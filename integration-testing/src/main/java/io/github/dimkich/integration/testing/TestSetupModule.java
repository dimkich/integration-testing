package io.github.dimkich.integration.testing;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.databind.cfg.HandlerInstantiator;
import com.fasterxml.jackson.databind.ser.PropertyFilter;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.diff.renderer.DiffRenderer;
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

/**
 * Module for configuring additional test setup options.
 * <p>
 * Provides registration of Jackson modules and filters, custom equality
 * predicates, and copy/cloning actions, as well as subtype and alias
 * information used during test initialization and serialization.
 */
@Getter
public class TestSetupModule {
    private final List<Class<?>> parentTypes = new ArrayList<>();
    private final List<Pair<Class<?>, String>> subTypesWithName = new ArrayList<>();
    private final List<Class<?>> subTypes = new ArrayList<>();
    private final List<Pair<Class<?>, String>> aliases = new ArrayList<>();
    private final List<Pair<Class<?>, String>> baseTypes = new ArrayList<>();
    private final List<com.fasterxml.jackson.databind.Module> jacksonModules = new ArrayList<>();
    private final List<Pair<String, PropertyFilter>> jacksonFilters = new ArrayList<>();
    private final Map<Class<?>, BiPredicate<?, ?>> equalsMap = new HashMap<>();
    private final Map<Class<?>, BiPredicate<?, ?>> presetEqualsMap = new HashMap<>();
    private final Map<Field, CopyAction> fieldActions = new LinkedHashMap<>();
    private final Map<Class<?>, CopyAction> typeActions = new LinkedHashMap<>();
    private final Map<Predicate<Class<?>>, CopyAction> predicateTypeActions = new LinkedHashMap<>();
    private final List<Pair<Class<?>, DiffPresenter<?>>> diffPresenters = new ArrayList<>();
    /**
     * Key presenters registered by this module, by key type.
     */
    private final List<Pair<Class<?>, DiffPresenter<?>>> diffKeyPresenters = new ArrayList<>();
    /**
     * Renderers of body types registered by this module.
     */
    private final List<Pair<Class<?>, DiffRenderer<?>>> diffRenderers = new ArrayList<>();
    private HandlerInstantiator handlerInstantiator;

    /**
     * Registers a parent type that can be used as a base for discovered subtypes.
     *
     * @param type the parent class or interface
     * @return this module for chaining
     */
    public TestSetupModule addParentType(Class<?> type) {
        parentTypes.add(type);
        return this;
    }

    /**
     * Registers a specific subtype with an explicit logical name.
     *
     * @param subType the subtype class
     * @param name    a unique name for the subtype
     * @return this module for chaining
     */
    public TestSetupModule addSubTypes(Class<?> subType, String name) {
        subTypesWithName.add(Pair.of(subType, name));
        return this;
    }

    /**
     * Registers one or more subtype classes.
     *
     * @param classes subtype classes to register
     * @return this module for chaining
     */
    public TestSetupModule addSubTypes(Class<?>... classes) {
        subTypes.addAll(Arrays.asList(classes));
        return this;
    }

    /**
     * Registers subtypes from an existing {@link JsonSubTypes} annotation.
     *
     * @param jsonSubTypes the annotation instance
     * @return this module for chaining
     */
    public TestSetupModule addSubTypes(JsonSubTypes jsonSubTypes) {
        for (JsonSubTypes.Type type : jsonSubTypes.value()) {
            addSubTypes(type.value(), type.name());
        }
        return this;
    }

    /**
     * Scans the given package and registers all found classes as subtypes,
     * excluding none.
     *
     * @param packageName the base package to scan
     * @return this module for chaining
     */
    public TestSetupModule addSubTypes(String packageName) {
        return addSubTypes(packageName, Set.of());
    }

    /**
     * Scans the given package and registers all found classes as subtypes,
     * excluding the specified classes.
     *
     * @param packageName the base package to scan
     * @param exclude     classes to be excluded from registration
     * @return this module for chaining
     */
    public TestSetupModule addSubTypes(String packageName, Set<Class<?>> exclude) {
        return addSubTypes(packageName, exclude, false);
    }

    /**
     * Scans the given package and registers all found classes as subtypes.
     *
     * @param packageName         the base package to scan
     * @param exclude             classes to be excluded from registration
     * @param includeInnerClasses whether to include inner classes in scan results
     * @return this module for chaining
     */
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

    /**
     * Adds an alias for a given subtype.
     *
     * @param subType the subtype class
     * @param alias   alternative name for the subtype
     * @return this module for chaining
     */
    public TestSetupModule addAlias(Class<?> subType, String alias) {
        aliases.add(Pair.of(subType, alias));
        return this;
    }

    /**
     * Adds a base type. All subtypes of a given base type will have the same name as the base type.
     *
     * @param baseType the base type class
     * @return this module for chaining
     */
    public TestSetupModule addBaseType(Class<?> baseType) {
        baseTypes.add(Pair.of(baseType, baseType.getSimpleName()));
        return this;
    }

    /**
     * Adds a base type. All subtypes of a given base type will have the same name as the base type.
     *
     * @param baseType the base type class
     * @param name     a unique name for the base type
     * @return this module for chaining
     */
    public TestSetupModule addBaseType(Class<?> baseType, String name) {
        baseTypes.add(Pair.of(baseType, name));
        return this;
    }

    /**
     * Registers a Jackson {@link com.fasterxml.jackson.databind.Module}.
     *
     * @param jacksonModule the module to register
     * @return this module for chaining
     */
    public TestSetupModule addJacksonModule(com.fasterxml.jackson.databind.Module jacksonModule) {
        jacksonModules.add(jacksonModule);
        return this;
    }

    /**
     * Registers a Jackson {@link PropertyFilter} with the given id.
     *
     * @param id     the filter id
     * @param filter the filter instance
     * @return this module for chaining
     */
    public TestSetupModule addJacksonFilter(String id, PropertyFilter filter) {
        jacksonFilters.add(Pair.of(id, filter));
        return this;
    }

    /**
     * Sets the Jackson {@link HandlerInstantiator} to be used.
     *
     * @param handlerInstantiator the handler instantiator
     * @return this module for chaining
     */
    public TestSetupModule setHandlerInstantiator(HandlerInstantiator handlerInstantiator) {
        this.handlerInstantiator = handlerInstantiator;
        return this;
    }

    /**
     * Registers a custom equals predicate for a given type.
     *
     * @param type   the target type
     * @param equals equality predicate for comparing two instances of the type
     * @param <T>    the type parameter
     * @return this module for chaining
     */
    public <T> TestSetupModule addEqualsForType(Class<T> type, BiPredicate<? super T, ? super T> equals) {
        equalsMap.put(type, equals);
        return this;
    }

    /**
     * Registers an equality preset of the framework.
     * <p>
     * A preset is used for comparing values like a custom predicate, but it does not suppress a
     * difference on its own: the renderer of the value decides, so metadata attributes and a
     * changed body stay visible. User code should register its own predicates with
     * {@link #addEqualsForType(Class, BiPredicate)}; this method is intended for framework
     * modules.
     *
     * @param type   the target type
     * @param equals equality predicate of the preset
     * @param <T>    the type parameter
     * @return this module for chaining
     */
    public <T> TestSetupModule addPresetEqualsForType(Class<T> type, BiPredicate<? super T, ? super T> equals) {
        presetEqualsMap.put(type, equals);
        return this;
    }

    /**
     * Registers a presenter that describes how a value type looks in the storage difference: its
     * presentation — body and metadata attributes — and its type. The difference itself is built by
     * the engine. A user registration of the same type replaces the built-in presenter.
     *
     * @param type      value type
     * @param presenter presenter of the type
     * @param <T>       value type
     * @return this module for chaining
     */
    public <T> TestSetupModule addDiffPresenter(Class<T> type, DiffPresenter<? super T> presenter) {
        diffPresenters.add(Pair.of(type, presenter));
        return this;
    }

    /**
     * Registers a presenter of a map key.
     * <p>
     * The form of the key is decided by the body returned by the presenter, using the same type
     * detector as for values: a scalar body is written as a text key, a structured body as an
     * object key with a type, attributes and body. A {@code null} body produces an
     * attributes-only object key.
     * <p>
     * A text key identifies a record inside its map and is never read back into an object. Its
     * text must be stable between runs and unique within one map; the engine fails with a clear
     * error when two keys of one map render the same text. An object key is a snapshot: the engine
     * never writes operations inside it.
     * <p>
     * Key and value presenters are looked up separately: a presenter registered with
     * {@link #addDiffPresenter(Class, DiffPresenter)} is not applied to a key automatically.
     *
     * @param type      key type
     * @param presenter presenter of the key
     * @param <T>       key type
     * @return this module for chaining
     */
    public <T> TestSetupModule addDiffKeyPresenter(Class<T> type, DiffPresenter<? super T> presenter) {
        diffKeyPresenters.add(Pair.of(type, presenter));
        return this;
    }

    /**
     * Registers the renderer of a body type.
     * <p>
     * The renderer is selected by the type of the body returned by a presenter. A body without a
     * registered renderer is rendered by its shape: a scalar or an array. A user registration of
     * the same type replaces the built-in renderer.
     * <p>
     * The type argument of the renderer is usually the registered type; it may be a subtype, which
     * keeps raw container classes registrable: {@code Map.class} with a
     * {@code DiffRenderer<Map<?, ?>>}. The renderer must accept every subtype of the registered
     * type: a renderer typed for {@code HashMap} registered under {@code Map.class} compiles, but
     * fails with a {@code ClassCastException} when the body is another map implementation.
     *
     * @param type     body type
     * @param renderer renderer of the body type
     * @param <T>      body type
     * @return this module for chaining
     */
    public <T> TestSetupModule addDiffRenderer(Class<T> type, DiffRenderer<? extends T> renderer) {
        diffRenderers.add(Pair.of(type, renderer));
        return this;
    }

    /**
     * Registers a cloner field action for a field identified by its declaring type and name.
     *
     * @param type   the declaring class
     * @param field  the field name
     * @param action the copy action to apply
     * @return this module for chaining
     */
    public TestSetupModule clonerFieldAction(Class<?> type, String field, CopyAction action) {
        clonerFieldAction(ReflectionUtils.getField(type, field), action);
        return this;
    }

    /**
     * Registers a cloner field action for the given field.
     *
     * @param field  the field reference
     * @param action the copy action to apply
     * @return this module for chaining
     */
    public TestSetupModule clonerFieldAction(Field field, CopyAction action) {
        fieldActions.put(field, action);
        return this;
    }

    /**
     * Registers a cloner type action for a specific class.
     *
     * @param type   the target type
     * @param action the copy action to apply
     * @return this module for chaining
     */
    public TestSetupModule clonerTypeAction(Class<?> type, CopyAction action) {
        typeActions.put(type, action);
        return this;
    }

    /**
     * Registers a cloner type action for types matching the given predicate.
     *
     * @param type   predicate to test candidate classes
     * @param action the copy action to apply
     * @return this module for chaining
     */
    public TestSetupModule clonerTypeAction(Predicate<Class<?>> type, CopyAction action) {
        predicateTypeActions.put(type, action);
        return this;
    }
}
