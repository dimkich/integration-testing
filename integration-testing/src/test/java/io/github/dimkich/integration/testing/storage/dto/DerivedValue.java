package io.github.dimkich.integration.testing.storage.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

/**
 * Subtype of {@link BaseValue}: the presenter of the base type wins over the default projection,
 * so the own fields of the subtype stay out of the difference.
 */
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class DerivedValue extends BaseValue {
    private String detail;

    public DerivedValue(String payload, String detail) {
        super(payload);
        this.detail = detail;
    }
}
