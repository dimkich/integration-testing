package io.github.dimkich.integration.testing.storage.diff;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresentation;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Resolves the presentation of a value or a key.
 * <p>
 * The service applies the presenter of the source type, then the presenter of the body's own type,
 * until the body type resolves to the presenter that produced it. Each presenter adds its metadata
 * attributes to the same builder; the result is a {@link DiffPresentation} with the final raw body
 * and all attributes of the chain.
 * <p>
 * The service is the only place that looks up value and key presenters. It does not know about
 * before and after, does not render differences and does not use the difference engine.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DiffPresentationService {
    private final DiffRegistry registry;

    /**
     * @param value value to present, may be {@code null} for an absent value
     * @return presentation of the value, or the absent presentation for {@code null}
     */
    DiffPresentation presentValue(Object value) {
        if (value == null) {
            return DiffPresentation.absent();
        }
        DiffPresenter<Object> rootPresenter = registry.findValuePresenter(value.getClass());
        return present(value, rootPresenter);
    }

    /**
     * @param key key to present
     * @return presentation of the key
     */
    DiffPresentation presentKey(Object key) {
        DiffPresenter<Object> rootPresenter = registry.findKeyPresenter(key.getClass());
        return present(key, rootPresenter);
    }

    /**
     * Returns the type of the value without building its presentation. A removed value asks only
     * for the type.
     *
     * @param value value to inspect
     * @param types resolver of the semantic type name
     * @return type name, or {@code null} when the element has no explicit type
     */
    String type(Object value, TypeNames types) {
        return value == null
                ? null
                : registry.findValuePresenter(value.getClass()).type(value, null, types);
    }

    private DiffPresentation present(Object source, DiffPresenter<Object> rootPresenter) {
        DiffAttributes.Builder attributes = DiffAttributes.builder();
        Object presentedBody = rootPresenter.present(source, attributes);
        return present(source, presentedBody, rootPresenter, rootPresenter, attributes);
    }

    private DiffPresentation present(Object source, Object presentedBody,
                                     DiffPresenter<Object> currentPresenter,
                                     DiffPresenter<Object> rootPresenter,
                                     DiffAttributes.Builder attributes) {
        if (presentedBody == null) {
            return new DiffPresentation(source, null, attributes.build(), rootPresenter);
        }
        DiffPresenter<Object> presenter = registry.findValuePresenter(presentedBody.getClass());
        if (presenter == currentPresenter) {
            return new DiffPresentation(source, presentedBody, attributes.build(), rootPresenter);
        }
        Object nextBody = presenter.present(presentedBody, attributes);
        return present(source, nextBody, presenter, rootPresenter, attributes);
    }
}
