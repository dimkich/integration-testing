package io.github.dimkich.integration.testing.storage.diff.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;

import java.util.Map;

/**
 * Describes a container as its own content: a map as named fields, a set or a collection as
 * unnamed elements.
 * <p>
 * A map with fields is self-describing, so its type is dropped: the fields alone identify the
 * value. A map that is written as an empty snapshot keeps its type, otherwise it would be
 * indistinguishable from an empty object. A set or a collection has unnamed elements, so its type
 * is always written: it distinguishes a set from a list and a list from an array.
 */
public class ContainerDiffPresenter implements DiffPresenter<Object> {

    @Override
    public Object present(Object value, DiffAttributes.Builder attributes) {
        return value;
    }

    @Override
    public String type(Object value, DiffValue body, TypeNames types) {
        if (!(value instanceof Map<?, ?> map)) {
            return types.typeOf(value);
        }
        if (body != null && !body.isEmpty()) {
            return null;
        }
        return map.isEmpty() ? types.typeOf(value) : null;
    }
}
