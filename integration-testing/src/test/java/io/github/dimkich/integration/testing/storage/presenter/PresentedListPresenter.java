package io.github.dimkich.integration.testing.storage.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.presenter.DiffPresenter;
import io.github.dimkich.integration.testing.storage.dto.PresentedList;

import java.util.Map;

/**
 * Describes a presented list by its size: the body has its own presenter, so the engine has to
 * apply it before choosing a collection renderer.
 */
public class PresentedListPresenter implements DiffPresenter<PresentedList> {

    @Override
    public Object present(PresentedList value, DiffAttributes.Builder attributes) {
        return Map.of("size", value.size());
    }
}
