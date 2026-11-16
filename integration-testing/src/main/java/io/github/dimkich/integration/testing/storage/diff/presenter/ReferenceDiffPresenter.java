package io.github.dimkich.integration.testing.storage.diff.presenter;

import io.github.dimkich.integration.testing.storage.diff.model.DiffAttributes;
import io.github.dimkich.integration.testing.storage.diff.model.DiffValue;
import io.github.dimkich.integration.testing.storage.diff.model.TypeNames;
import lombok.RequiredArgsConstructor;

import java.util.function.Function;

/**
 * Describes a reference wrapper by the value it refers to: an empty optional, a reference and a
 * markable reference are written as their content, so the engine diffs the content by the usual
 * rules. The mark of {@code AtomicMarkableReference} is ignored, matching its equality preset.
 *
 * @param <T> wrapper type
 */
@RequiredArgsConstructor
public class ReferenceDiffPresenter<T> implements DiffPresenter<T> {
    private final Function<? super T, ?> reference;

    @Override
    public Object present(T value, DiffAttributes.Builder attributes) {
        return reference.apply(value);
    }

    @Override
    public String type(T value, DiffValue body, TypeNames types) {
        return types.typeOf(reference.apply(value));
    }
}
