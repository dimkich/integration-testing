package io.github.dimkich.integration.testing.storage.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.dto.AttributedValue;

/**
 * Presents a value with one configurable attribute: the tests use it to check attribute names
 * that the XML difference format reserves or cannot represent.
 */
public class AttributedValuePresenter implements DiffPresenter<AttributedValue> {

    @Override
    public Object present(AttributedValue value, DiffAttributes.Builder attributes) {
        attributes.addAttribute(value.getAttributeName(), value.getAttributeValue());
        return value.getBody();
    }
}
