package io.github.dimkich.integration.testing.storage.diff;

import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.config.SimpleTypeDetector;
import io.github.dimkich.integration.testing.storage.diff.presenter.DefaultDiffPresenter;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.diff.renderer.DiffRenderer;
import io.github.dimkich.integration.testing.storage.diff.renderer.ScalarDiffRenderer;
import io.github.dimkich.integration.testing.storage.diff.renderer.SnapshotDiffRenderer;
import io.github.dimkich.integration.testing.util.FastPath;
import io.github.dimkich.integration.testing.util.TypeSpecificity;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.lang.NonNull;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lookup of presenters and body renderers by type.
 * <p>
 * The registry looks up presenters by role. A value presenter describes a value: the most specific
 * registered presenter wins, and the fallback presenter takes over when nothing is registered. The
 * presenter of the body type projects the body, so a nested presentation is not lost, and then a
 * renderer of the body type renders it. A body without a registered renderer falls back to its
 * shape: a scalar or an array; every other body requires a registered renderer.
 * <p>
 * A key has its own presenter: a registered key presenter wins, so the same type can be presented
 * as a value and as a key differently, and the registry never applies a value presenter to a key
 * automatically. The built-in module registers the container presenter for both roles, so a
 * container keeps its shape as a value and as a key. The form of the key — text or object — does
 * not depend on the registration: it follows the body returned by the presenter, so a scalar body
 * becomes text and a structured body becomes an object.
 * <p>
 * A presenter or renderer registered for a type wins over one registered for a supertype:
 * {@link #init()} orders registrations by type specificity, so the most specific registration wins
 * regardless of registration order.
 * <p>
 * {@link #init()} assembles the registry from the {@link TestSetupModule}s in their injection
 * order; afterward the registry is read-only. The built-in module comes first, so a user module
 * that registers a presenter or renderer of the same type replaces the built-in one. The registry
 * resolves the value presenter, the key presenter and the registered renderer of a class once and
 * caches them together in a {@link ClassValue}, because the lookups run for every value, every map
 * key and every element of a difference.
 * <p>
 * The registry caches only registrations. The scalar/array fallback renderer depends on the
 * serializers of the current test format, so the registry resolves it on demand: XML and JSON can
 * run in the same context without the first format fixing the shape of the other.
 */
@RequiredArgsConstructor
class DiffRegistry {
    private final List<TestSetupModule> modules;
    private final DefaultDiffPresenter defaultPresenter;
    private final SimpleTypeDetector simpleTypeDetector;
    private final ScalarDiffRenderer scalarRenderer;
    private final SnapshotDiffRenderer<Object> snapshotRenderer = new SnapshotDiffRenderer<>();

    private ClassValue<Resolved> resolved;

    /**
     * Collects the presenters and renderers of all modules, orders the registrations by type
     * specificity and builds the resolver. Runs once per context; the registry is immutable after
     * that.
     */
    @PostConstruct
    void init() {
        Map<Class<?>, DiffPresenter<?>> valuePresenters = new LinkedHashMap<>();
        Map<Class<?>, DiffPresenter<?>> keyPresenters = new LinkedHashMap<>();
        Map<Class<?>, DiffRenderer<?>> renderers = new LinkedHashMap<>();
        for (TestSetupModule module : modules) {
            for (Pair<Class<?>, DiffPresenter<?>> presenter : module.getDiffPresenters()) {
                valuePresenters.put(presenter.getKey(), presenter.getValue());
            }
            for (Pair<Class<?>, DiffPresenter<?>> presenter : module.getDiffKeyPresenters()) {
                keyPresenters.put(presenter.getKey(), presenter.getValue());
            }
            for (Pair<Class<?>, DiffRenderer<?>> renderer : module.getDiffRenderers()) {
                renderers.put(renderer.getKey(), renderer.getValue());
            }
        }

        List<Registration<DiffPresenter<?>>> values = ordered(valuePresenters);
        List<Registration<DiffPresenter<?>>> keys = ordered(keyPresenters);
        List<Registration<DiffRenderer<?>>> rendererList = ordered(renderers);
        resolved = new ClassValue<>() {
            @Override
            protected Resolved computeValue(@NonNull Class<?> type) {
                DiffPresenter<Object> value = presenter(values, type);
                DiffPresenter<Object> key = presenter(keys, type);
                DiffRenderer<Object> renderer = renderer(rendererList, type);
                return new Resolved(
                        value != null ? value : defaultPresenter,
                        key != null ? key : defaultPresenter,
                        renderer);
            }
        };
    }

    /**
     * @param type value or body type
     * @return the most specific registered presenter or the fallback presenter
     */
    public DiffPresenter<Object> findValuePresenter(Class<?> type) {
        return resolved.get(type).valuePresenter();
    }

    /**
     * Resolves the presenter of a key type: a registered key presenter or the fallback presenter.
     * <p>
     * The body returned by the presenter decides whether the key is written as text or as an
     * object: a scalar body is text, a structured body is an object. The returned presenter is
     * never {@code null}.
     *
     * @param type key type
     * @return presenter of the key type
     */
    public DiffPresenter<Object> findKeyPresenter(Class<?> type) {
        return resolved.get(type).keyPresenter();
    }

    /**
     * @param type body type
     * @return renderer of the body type or the renderer of its scalar/array shape
     * @throws IllegalStateException when the body type has no registered renderer and is neither
     *                               a scalar nor an array
     */
    public DiffRenderer<Object> findRenderer(Class<?> type) {
        DiffRenderer<Object> renderer = resolved.get(type).renderer();
        if (renderer != null) {
            return renderer;
        }
        renderer = defaultRenderer(type);
        if (renderer == null) {
            throw new IllegalStateException("No diff renderer for body type " + type.getName()
                    + "; register one via TestSetupModule.addDiffRenderer");
        }
        return renderer;
    }

    /**
     * Selects the renderer of a body shape: a scalar or an array. The method runs on demand
     * because the shape depends on the serializers of the current test format.
     * <p>
     * The scalar check goes first: {@code byte[]} and {@code char[]} are written as scalar text,
     * while other arrays are rendered element by element.
     */
    private DiffRenderer<Object> defaultRenderer(Class<?> type) {
        if (FastPath.isScalar(type, simpleTypeDetector)) {
            return scalarRenderer;
        }
        return FastPath.isArray(type) ? snapshotRenderer : null;
    }

    /**
     * Reports whether the value type is a plain scalar: the default presenter, no registered
     * renderer and a scalar Jackson shape apply to it. Such a value needs no presentation and no
     * renderer: its body is its text, and it has no metadata attributes.
     *
     * @param type value type
     * @return {@code true} when the value can be written by the direct scalar path
     */
    boolean isPlainScalar(Class<?> type) {
        Resolved resolved = this.resolved.get(type);
        return resolved.valuePresenter() == defaultPresenter
                && resolved.renderer() == null
                && FastPath.isScalar(type, simpleTypeDetector);
    }

    /**
     * Reports whether the key type is a plain text key: both the value and the key presenter are
     * the defaults and the type is a scalar, so the key text is the value text directly.
     *
     * @param type key type
     * @return {@code true} when the key can be written without a presentation
     */
    boolean isPlainKey(Class<?> type) {
        Resolved resolved = this.resolved.get(type);
        return resolved.valuePresenter() == defaultPresenter
                && resolved.keyPresenter() == defaultPresenter
                && FastPath.isScalar(type, simpleTypeDetector);
    }

    private static <P> List<Registration<P>> ordered(Map<Class<?>, P> registrations) {
        List<Registration<P>> ordered = new ArrayList<>(registrations.size());
        registrations.forEach((type, item) -> ordered.add(new Registration<>(type, item)));
        ordered.sort((first, second) -> TypeSpecificity.compare(first.type(), second.type()));
        return ordered;
    }

    @SuppressWarnings("unchecked")
    private static DiffPresenter<Object> presenter(
            List<Registration<DiffPresenter<?>>> registrations, Class<?> type) {
        for (Registration<DiffPresenter<?>> registration : registrations) {
            if (registration.type().isAssignableFrom(type)) {
                return (DiffPresenter<Object>) registration.item();
            }
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static DiffRenderer<Object> renderer(
            List<Registration<DiffRenderer<?>>> registrations, Class<?> type) {
        for (Registration<DiffRenderer<?>> registration : registrations) {
            if (registration.type().isAssignableFrom(type)) {
                return (DiffRenderer<Object>) registration.item();
            }
        }
        return null;
    }

    private record Registration<P>(Class<?> type, P item) {
    }

    /**
     * Registrations of one type. The renderer is {@code null} when no renderer is registered for
     * the type; the scalar/array fallback is not cached here because it depends on the serializers
     * of the current test format.
     */
    private record Resolved(DiffPresenter<Object> valuePresenter,
                            DiffPresenter<Object> keyPresenter,
                            DiffRenderer<Object> renderer) {
    }
}
