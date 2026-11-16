package io.github.dimkich.integration.testing.storage.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.dto.CompactKey;

import java.util.Map;

/**
 * Object key of a compact key: only the identifier is written, the second field is intentionally
 * hidden.
 */
public class CompactKeyPresenter implements DiffPresenter<CompactKey> {

    @Override
    public Object present(CompactKey key, DiffAttributes.Builder attributes) {
        return Map.of("id", key.getId());
    }
}
