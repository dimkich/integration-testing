package io.github.dimkich.integration.testing.storage.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.dto.PresentedValue;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Exposes the body of a value and its optional {@code ttl} and {@code score} attributes. The body
 * is rendered by the usual rules, so the same fixture covers scalar, map, collection, array and
 * multi-value map bodies. An ordered presentation keeps the declaration order of the attributes;
 * an unordered one is sorted by the engine.
 */
public class PresentedValuePresenter implements DiffPresenter<PresentedValue> {

    @Override
    public Object present(PresentedValue value, DiffAttributes.Builder attributes) {
        attributes.addAttributes(attributeValues(value));
        return value.getBody();
    }

    private static Map<String, String> attributeValues(PresentedValue value) {
        if (value.isOrderedAttributes()) {
            Map<String, String> attributes = new LinkedHashMap<>(2);
            if (value.getTtl() != null) {
                attributes.put("ttl", value.getTtl());
            }
            if (value.getScore() != null) {
                attributes.put("score", value.getScore());
            }
            return attributes;
        }
        if (value.getTtl() == null) {
            return value.getScore() == null ? Map.of() : Map.of("score", value.getScore());
        }
        if (value.getScore() == null) {
            return Map.of("ttl", value.getTtl());
        }
        return Map.of("ttl", value.getTtl(), "score", value.getScore());
    }
}
