package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.format.common.polymorphic.unwrapped.UnwrappedTypeProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Unwrapped polymorphic value whose type attribute is renamed per field.
 */
@Data
@NoArgsConstructor
public class UnwrappedTypeField {
    @JacksonXmlProperty(isAttribute = true)
    private String key;

    @JsonUnwrapped
    @UnwrappedTypeProperty("kind")
    private Object value;

    public UnwrappedTypeField(String key, Object value) {
        this.key = key;
        this.value = value;
    }
}
