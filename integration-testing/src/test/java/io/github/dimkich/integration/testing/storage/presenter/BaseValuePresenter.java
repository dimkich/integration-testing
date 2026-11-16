package io.github.dimkich.integration.testing.storage.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.dto.BaseValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Describes a base value: the presenter is registered for the base type, so it applies to every
 * subtype and hides the subtype fields.
 */
public class BaseValuePresenter implements DiffPresenter<BaseValue> {

    @Override
    public Object present(BaseValue value, DiffAttributes.Builder attributes) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("payload", value.getPayload());
        attributes.addAttribute("presented", "base");
        return body;
    }

    @Override
    public String type(BaseValue value, DiffValue body, TypeNames types) {
        return "BaseValue";
    }
}
