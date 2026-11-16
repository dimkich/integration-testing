package io.github.dimkich.integration.testing.storage.diff.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;

/**
 * Describes how a concrete type looks in a storage difference: its presentation — body and
 * metadata attributes — and its type.
 * <p>
 * The same interface describes values and keys. A value presenter describes a value that is
 * compared before and after the test; a key presenter describes a key whose body is always a
 * snapshot. For a key the body also decides the form: a scalar body is written as text, a
 * structured body as an object key.
 * <p>
 * The body is raw data — a map, a collection, an array or a scalar — not a difference. An object
 * with properties is presented as a map; the default presenter does it automatically.
 * The engine compares the bodies of the values before and after the test and writes the result by
 * the general rules, so a presenter never builds a difference itself. The difference of the body
 * is built by a {@code DiffRenderer} selected by the body type.
 * <p>
 * A presenter is called only for the parts the engine needs: a removed value asks only for the
 * type, metadata attributes are never written for a removed value, and for a text key only the
 * body is used — type and attributes belong to the object form.
 *
 * @param <T> presented type
 */
@FunctionalInterface
public interface DiffPresenter<T> {

    /**
     * Builds the raw body of a value and adds its metadata attributes to the builder.
     * <p>
     * The engine calls this method for every value it writes: an inserted or updated value and a
     * key. A presenter that projects a value (for example through Jackson) builds the body and the
     * attributes from one projection, so the value is not projected twice.
     * <p>
     * The body is rendered by the usual rules, so it may be a map, a collection, an array or a
     * scalar; an object with properties is presented as a map. A {@code null} body means that the
     * element has only attributes.
     *
     * @param value      value to represent
     * @param attributes builder of metadata attributes, never {@code null}
     * @return raw body of the value, or {@code null} for an attributes-only element
     */
    Object present(T value, DiffAttributes.Builder attributes);

    /**
     * Returns the type name written as the {@code type} attribute.
     * <p>
     * The default implementation returns the type of the value itself, so an element that owns its
     * body needs no override. The rendered body is passed because an element with fields is
     * self-describing and needs no type: a presenter may use it to drop the type of the map that is
     * written only as deleted or changed fields.
     * <p>
     * A presenter that exposes another value as the body, for example a Redis hash written as a map
     * or a wrapper written as its payload, overrides this method to keep the type of that body.
     * <p>
     * When the engine checks whether a value changed its type, it calls this method with a
     * {@code null} body, because an unchanged value contributes only its type to the difference.
     *
     * @param value value to represent
     * @param body  rendered body, or {@code null} for a removed value, an element without
     *              a body or an unchanged element whose type is compared
     * @param types resolver of the semantic type name
     * @return type name, or {@code null} when the element has no explicit type
     */
    default String type(T value, DiffValue body, TypeNames types) {
        return types.typeOf(value);
    }
}
