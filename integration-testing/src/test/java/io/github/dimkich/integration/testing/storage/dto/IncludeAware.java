package io.github.dimkich.integration.testing.storage.dto;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * Value with fields included by their own {@link JsonInclude} rules, an unwrapped nested object
 * and an any-getter: the difference body has to repeat the serialized shape of the value.
 */
@Data
@NoArgsConstructor
public class IncludeAware {
    @Getter(onMethod_ = @JsonInclude(JsonInclude.Include.NON_EMPTY))
    private String text;

    @Getter(onMethod_ = @JsonInclude(JsonInclude.Include.NON_EMPTY))
    private Map<String, Object> map;

    @Getter(onMethod_ = @JsonInclude(JsonInclude.Include.NON_EMPTY))
    private Integer number;

    /**
     * Always written, even when {@code null}: a bare {@code @JsonInclude} resets the inclusion to
     * {@code ALWAYS} and overrides the mapper-wide {@code NON_NULL}.
     */
    @Getter(onMethod_ = @JsonInclude)
    private String always;

    private String anyValue;

    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String writeOnly;

    @JsonUnwrapped
    private Nested nested;

    /**
     * Always empty: an empty collection has to be skipped by its include rule.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public List<String> getList() {
        return List.of();
    }

    /**
     * Always empty: an empty array has to be skipped by its include rule.
     */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public int[] getArray() {
        return new int[0];
    }

    @JsonAnyGetter
    public Map<String, Object> getExtra() {
        return anyValue == null ? Map.of() : Map.of("extra", anyValue);
    }

    /**
     * Nested object of the unwrapped property.
     */
    @Data
    @NoArgsConstructor
    public static class Nested {
        private String first;
        private String second;
    }
}
