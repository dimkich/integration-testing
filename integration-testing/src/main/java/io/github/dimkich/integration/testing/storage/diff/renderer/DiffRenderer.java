package io.github.dimkich.integration.testing.storage.diff.renderer;

import io.github.dimkich.integration.testing.storage.diff.ObjectsDifference;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;

/**
 * Builds the difference of a value body.
 * <p>
 * A renderer is selected by the type of the body after the test. The body before the test comes
 * from the presenter of the previous value, so it may be {@code null} or have another type. The
 * renderer itself decides how to treat a body of another shape: the built-in container renderers
 * compare it with an empty container, so every element of the new body is written as added. A
 * scalar is written as its value and a sequence as a snapshot, because a sequence has no element
 * identity. Type and metadata attributes are provided by the value presenter, not by the renderer.
 * <p>
 * A renderer is the source of truth for "changed". When the body before the test is present and
 * the rendered bodies are equal, the renderer returns {@code null}. A snapshot, including an
 * empty one, is never {@code null}: an absent value and an empty value are different things.
 * <p>
 * A changed field is built by {@link ObjectsDifference#update}; the writer derives the actual tag
 * from the enclosing element.
 *
 * @param <T> body type after the test
 */
@FunctionalInterface
public interface DiffRenderer<T> {

    /**
     * @param before     body before the test produced by the presenter of the previous value,
     *                   may be {@code null} or have another type
     * @param after      body after the test, never {@code null}
     * @param difference difference engine of the current build
     * @return rendered body, or {@code null} when {@code before} is present and the rendered
     *         bodies are equal
     */
    DiffValue render(Object before, T after, ObjectsDifference difference);

    /**
     * Renders the body when the body itself did not change but the metadata attributes did.
     * <p>
     * The default renders the whole value as a snapshot, which is right for a renderer that
     * writes values rather than differences. A renderer that writes only changes must override
     * this method and return {@code null}: an empty difference is written without a body at all.
     *
     * @param body       body after the test, never {@code null}
     * @param difference difference engine of the current build
     * @return whole body, or {@code null} when this renderer writes differences
     */
    default DiffValue renderUnchanged(T body, ObjectsDifference difference) {
        return render(null, body, difference);
    }
}
