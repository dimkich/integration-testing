package io.github.dimkich.integration.testing.storage.diff;

import io.github.dimkich.integration.testing.config.SimpleTypeDetector;
import io.github.dimkich.integration.testing.storage.diff.format.ValueFormat;
import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffEntry;
import io.github.dimkich.integration.testing.storage.diff.model.DiffKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffObjectKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffOperation;
import io.github.dimkich.integration.testing.storage.diff.model.DiffScalarValue;
import io.github.dimkich.integration.testing.storage.diff.model.DiffTextKey;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresentation;
import io.github.dimkich.integration.testing.storage.diff.renderer.DiffRenderer;
import io.github.dimkich.integration.testing.util.FastPath;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

/**
 * Renders presentations through the renderer registered for the body type.
 * <p>
 * The service is the only place that looks up body renderers. It combines the renderer result with
 * the presentation state and builds the entry: the renderer renders the body, the service decides
 * whether the element produced no entry, an entry without a body, or a body.
 */
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
class DiffRenderService {
    private final DiffRegistry registry;
    private final DiffPresentationService presentations;
    private final SimpleTypeDetector simpleTypeDetector;
    private final ValueFormat valueFormat;

    /**
     * Renders the element after the test.
     *
     * @param operation  operation applied to the element
     * @param key        identifier of the element within its parent
     * @param before     presentation before the test; an absent value is an empty presentation
     * @param after      presentation after the test
     * @param difference difference engine of the current build
     * @return entry to write, or {@code null} when the element did not change
     */
    DiffEntry renderEntry(DiffOperation operation, DiffKey key, DiffPresentation before,
                          DiffPresentation after, ObjectsDifference difference) {
        DiffValue value;
        Object beforeBody = before.presentedBody();
        Object afterBody = after.presentedBody();
        if (afterBody == null) {
            if (isSameElement(before, after)) {
                return null;
            }
            value = null;
        } else {
            DiffRenderer<Object> renderer = registry.findRenderer(afterBody.getClass());
            value = renderer.render(beforeBody, afterBody, difference);
            if (value == null) {
                if (isSameElement(before, after)) {
                    return null;
                }
                value = renderer.renderUnchanged(afterBody, difference);
            }
        }
        return new DiffEntry(operation, key, after.type(value, valueFormat),
                after.attributes(), value);
    }

    /**
     * @param before presentation before the test
     * @param after  presentation after the test
     * @return {@code true} when both presentations have the same body presence, attributes and type
     */
    private boolean isSameElement(DiffPresentation before, DiffPresentation after) {
        if ((before.presentedBody() == null) != (after.presentedBody() == null)) {
            return false;
        }
        return before.attributesAndTypesEquals(after, valueFormat);
    }

    /**
     * Reports whether the value type is written by the direct scalar path.
     *
     * @param type value type
     * @return {@code true} when the value needs no presentation and no renderer
     */
    boolean isPlainScalar(Class<?> type) {
        return registry.isPlainScalar(type);
    }

    /**
     * Builds a scalar element without resolving its full presentation: a plain scalar has no
     * metadata attributes and the scalar renderer writes only its text. The type name is still
     * resolved from the value.
     *
     * @param operation operation applied to the element
     * @param key       identifier of the element within its parent
     * @param value     scalar value
     * @return element with a scalar body
     */
    DiffEntry scalarEntry(DiffOperation operation, DiffKey key, Object value) {
        return new DiffEntry(operation, key, presentations.type(value, valueFormat),
                DiffAttributes.EMPTY, new DiffScalarValue(valueFormat.text(value)));
    }

    /**
     * Renders a map key as a difference identifier: a scalar body becomes a text key, a structured
     * body an object key. The key body is a snapshot, so the renderer receives no before body.
     *
     * @param key map key, may be {@code null}
     * @return rendered key
     */
    DiffKey renderKey(Object key, ObjectsDifference difference) {
        if (key == null) {
            return new DiffTextKey("");
        }
        if (registry.isPlainKey(key.getClass())) {
            return new DiffTextKey(valueFormat.text(key));
        }
        DiffPresentation presentation = presentations.presentKey(key);
        Object body = presentation.presentedBody();
        if (body != null && FastPath.isScalar(body.getClass(), simpleTypeDetector)) {
            return new DiffTextKey(valueFormat.text(body));
        }
        DiffValue value = body == null ? null : render(null, body, difference);
        return new DiffObjectKey(presentation.type(value, valueFormat),
                presentation.attributes(), value);
    }

    /**
     * Body pair with no presentation: the storage snapshot itself.
     */
    DiffValue render(Object beforeBody, Object afterBody, ObjectsDifference difference) {
        return afterBody == null
                ? null
                : registry.findRenderer(afterBody.getClass())
                        .render(beforeBody, afterBody, difference);
    }
}
