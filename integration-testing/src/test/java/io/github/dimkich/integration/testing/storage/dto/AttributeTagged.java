package io.github.dimkich.integration.testing.storage.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value with a property serialized as an XML attribute: the attribute is written on the difference
 * element itself, and the remaining properties form the body.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AttributeTagged {
    @JacksonXmlProperty(isAttribute = true)
    @JsonInclude
    private String version;
    private String payload;
}
