package io.github.dimkich.integration.testing.storage.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntryStringKeyObjectValue implements Comparable<EntryStringKeyObjectValue> {
    @JacksonXmlProperty(isAttribute = true)
    private String key;
    @JacksonXmlProperty(isAttribute = true)
    private Container.ChangeType change;
    @JsonUnwrapped
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Object value;

    @Override
    public int compareTo(EntryStringKeyObjectValue o) {
        return key.compareTo(o.key);
    }
}
