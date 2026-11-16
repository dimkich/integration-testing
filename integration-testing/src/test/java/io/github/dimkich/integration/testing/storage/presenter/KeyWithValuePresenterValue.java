package io.github.dimkich.integration.testing.storage.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.dto.KeyWithValuePresenter;

import java.util.Map;

/**
 * Value presenter of a type that is also registered as an object key without a key presenter.
 */
public class KeyWithValuePresenterValue implements DiffPresenter<KeyWithValuePresenter> {

    @Override
    public Object present(KeyWithValuePresenter value, DiffAttributes.Builder attributes) {
        return Map.of("presented", value.getId());
    }

    @Override
    public String type(KeyWithValuePresenter value, DiffValue body, TypeNames types) {
        return "ValuePresenter";
    }
}
