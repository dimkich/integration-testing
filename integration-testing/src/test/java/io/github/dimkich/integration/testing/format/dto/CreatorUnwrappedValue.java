package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unwrapped polymorphic value in a bean that has an all-args constructor.
 * <p>
 * Because {@code lombok.anyConstructor.addConstructorProperties=true}, the all-args constructor
 * makes Jackson use a property-based creator. Unknown properties, including the unwrapped type
 * attribute, are then replayed after the payload, so the type id must not be expected to be the
 * first token of the buffered payload.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreatorUnwrappedValue {
    @JacksonXmlProperty(isAttribute = true)
    private String key;

    @JsonUnwrapped
    private Object value;
}
