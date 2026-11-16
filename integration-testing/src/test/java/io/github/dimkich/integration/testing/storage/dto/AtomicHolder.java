package io.github.dimkich.integration.testing.storage.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.concurrent.atomic.AtomicMarkableReference;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Holder of atomic references: the comparison presets compare the referenced values, so the holder
 * exposes them as text and builds the atomics itself.
 */
@Data
@NoArgsConstructor
public class AtomicHolder {
    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private AtomicReference<String> reference;

    @Getter(AccessLevel.NONE)
    @Setter(AccessLevel.NONE)
    private AtomicMarkableReference<String> markable;

    @JsonProperty("reference")
    public String getReferenceValue() {
        return reference == null ? null : reference.get();
    }

    @JsonProperty("reference")
    public void setReferenceValue(String value) {
        reference = value == null ? null : new AtomicReference<>(value);
    }

    @JsonProperty("markable")
    public String getMarkableValue() {
        return markable == null ? null : markable.getReference();
    }

    @JsonProperty("markable")
    public void setMarkableValue(String value) {
        markable = value == null ? null : new AtomicMarkableReference<>(value, false);
    }
}
