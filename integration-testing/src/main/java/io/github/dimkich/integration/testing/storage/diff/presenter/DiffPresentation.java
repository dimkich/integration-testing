package io.github.dimkich.integration.testing.storage.diff.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;
import lombok.NonNull;

import java.util.Objects;

/**
 * Presentation of a value or a key resolved through the presenter chain.
 * <p>
 * The source is the original value or key, the presented body is the final body after the
 * presenters of the body type have been applied, and the root presenter is the presenter of the
 * source type. The root presenter supplies the type; the body presenters are an internal part of
 * the chain and are not exposed.
 * <p>
 * The body is raw data, not a difference: the engine renders it by the usual rules. A {@code null}
 * body means that the element has only attributes.
 * <p>
 * Attributes are all attributes of the chain, not a difference: an ordered map keeps its order,
 * an unordered one is sorted by name when the chain is built. A caller that compares two
 * presentations asks {@link #attributesAndTypesEquals(DiffPresentation, TypeNames)} for
 * metadata.
 * <p>
 * The singleton {@link #absent()} represents an absent value: it has neither a body nor attributes
 * nor a type. It is an empty presentation, so a value presented without any data is
 * indistinguishable from an absent value.
 *
 * @param source        original value or key, never {@code null}
 * @param presentedBody final body after the presenter chain, or {@code null} for an
 *                      attributes-only element
 * @param attributes    metadata attributes by name, never {@code null}
 * @param rootPresenter presenter of the source type, never {@code null}
 */
public record DiffPresentation(@NonNull Object source,
                               Object presentedBody,
                               DiffAttributes attributes,
                               @NonNull DiffPresenter<Object> rootPresenter) {
    private static final Object ABSENT_SOURCE = new Object();
    private static final DiffPresentation ABSENT =
            new DiffPresentation(ABSENT_SOURCE, null, DiffAttributes.EMPTY, new AbsentPresenter());

    public DiffPresentation {
        attributes = attributes == null ? DiffAttributes.EMPTY : attributes;
    }

    /**
     * @return presentation of an absent value, without a body, attributes or type
     */
    public static DiffPresentation absent() {
        return ABSENT;
    }

    /**
     * Returns the type name of the element.
     *
     * @param rendered rendered body, or {@code null} for an element without a body or an
     *                 unchanged element whose type is compared
     * @param types    resolver of the semantic type name
     * @return type name, or {@code null} when the element has no explicit type
     */
    public String type(DiffValue rendered, TypeNames types) {
        return rootPresenter.type(source, rendered, types);
    }

    /**
     * Compares the metadata of two presentations: attributes and type. The body is not compared
     * here; the renderer compares its content and the render service compares its presence.
     *
     * @param after presentation after the test
     * @param types resolver of the semantic type name
     * @return {@code true} when the attributes and type names are equal
     */
    public boolean attributesAndTypesEquals(DiffPresentation after, TypeNames types) {
        return attributes.equals(after.attributes)
                && Objects.equals(type(null, types), after.type(null, types));
    }

    /**
     * Presenter of an absent value. It supplies no type; the presentation is never passed to a
     * renderer.
     */
    private static final class AbsentPresenter implements DiffPresenter<Object> {
        @Override
        public Object present(Object value, DiffAttributes.Builder attributes) {
            throw new IllegalStateException("An absent value has no presentation");
        }

        @Override
        public String type(Object value, DiffValue body, TypeNames types) {
            return null;
        }
    }
}
