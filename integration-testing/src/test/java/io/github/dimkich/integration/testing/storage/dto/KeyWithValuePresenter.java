package io.github.dimkich.integration.testing.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Type registered both as a value presenter and as an object key without an object presenter.
 * The key has to use the default projection, not the value presenter.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyWithValuePresenter {
    private String id;
}
