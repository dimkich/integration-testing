package io.github.dimkich.integration.testing.storage.diff;

import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.storage.diff.format.ValueFormat;
import io.github.dimkich.integration.testing.storage.diff.presenter.ContainerDiffPresenter;
import io.github.dimkich.integration.testing.storage.diff.presenter.DefaultDiffPresenter;
import io.github.dimkich.integration.testing.storage.diff.presenter.ReferenceDiffPresenter;
import io.github.dimkich.integration.testing.storage.diff.renderer.MapDiffRenderer;
import io.github.dimkich.integration.testing.storage.diff.renderer.MultiValueMapDiffRenderer;
import io.github.dimkich.integration.testing.storage.diff.renderer.ScalarDiffRenderer;
import io.github.dimkich.integration.testing.storage.diff.renderer.SetDiffRenderer;
import io.github.dimkich.integration.testing.storage.diff.renderer.SnapshotDiffRenderer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.util.MultiValueMap;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Registers the beans of the difference engine and the module with the built-in presenters and
 * renderers.
 * <p>
 * A built-in presenter or renderer is a bean only when it depends on another bean: the default
 * presenter and the scalar renderer. The rest are stateless, so they are created directly.
 */
@Configuration
@Import({ObjectsDifference.class, DiffRegistry.class, DiffPresentationService.class,
        DiffRenderService.class, ValueFormat.class, DefaultDiffPresenter.class,
        ScalarDiffRenderer.class})
public class DiffConfig {
    /**
     * Registers the built-in presenters and renderers of the difference engine.
     * <p>
     * The container presenter is registered both as a value presenter and as a key presenter, so
     * a container keeps its shape when it is used as a map key, while a user value presenter is
     * still not applied to a key automatically.
     * <p>
     * The module is ordered first, so a user module that registers the same type replaces the
     * built-in registration.
     */
    @Bean
    @Order(Ordered.HIGHEST_PRECEDENCE)
    TestSetupModule diffModule() {
        ContainerDiffPresenter containerPresenter = new ContainerDiffPresenter();
        return new TestSetupModule()
                .addDiffPresenter(Map.class, containerPresenter)
                .addDiffPresenter(Set.class, containerPresenter)
                .addDiffPresenter(Collection.class, containerPresenter)
                .addDiffPresenter(Optional.class,
                        new ReferenceDiffPresenter<Object>(value -> ((Optional<?>) value).orElse(null)))
                .addDiffPresenter(AtomicReference.class,
                        new ReferenceDiffPresenter<Object>(value -> ((AtomicReference<?>) value).get()))
                .addDiffPresenter(AtomicMarkableReference.class,
                        new ReferenceDiffPresenter<Object>(
                                value -> ((AtomicMarkableReference<?>) value).getReference()))
                .addDiffKeyPresenter(Map.class, containerPresenter)
                .addDiffKeyPresenter(Set.class, containerPresenter)
                .addDiffKeyPresenter(Collection.class, containerPresenter)
                .addDiffRenderer(MultiValueMap.class, new MultiValueMapDiffRenderer())
                .addDiffRenderer(Map.class, new MapDiffRenderer())
                .addDiffRenderer(Set.class, new SetDiffRenderer())
                .addDiffRenderer(Collection.class, new SnapshotDiffRenderer<Collection<?>>());
    }
}
