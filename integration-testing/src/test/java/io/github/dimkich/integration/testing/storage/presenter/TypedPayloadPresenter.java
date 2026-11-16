package io.github.dimkich.integration.testing.storage.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.dto.TypedPayload;

/**
 * Exposes the payload itself: the type of the element is taken from the payload, so the type of
 * the wrapper never appears in the expected data.
 */
public class TypedPayloadPresenter implements DiffPresenter<TypedPayload> {

    @Override
    public Object present(TypedPayload value, DiffAttributes.Builder attributes) {
        return value.getPayload();
    }

    @Override
    public String type(TypedPayload value, DiffValue body, TypeNames types) {
        return types.typeOf(value.getPayload());
    }
}
