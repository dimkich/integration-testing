package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.*;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class Value {
    @JacksonXmlProperty(isAttribute = true)
    private String attr;
    private Object value;

    public Value(Object value) {
        this.value = value;
    }

    public Value(String attr) {
        this.attr = attr;
    }
}
