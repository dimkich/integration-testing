package io.github.dimkich.integration.testing.storage.mapping;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntryObjectKeyObjectValue implements Comparable<EntryObjectKeyObjectValue> {
    @JacksonXmlProperty(isAttribute = true)
    private Container.ChangeType change;
    private Object key;
    @JsonInclude(JsonInclude.Include.ALWAYS)
    private Object value;

    @Override
    public int compareTo(EntryObjectKeyObjectValue o) {
        if (key instanceof Comparable comparable) {
            return comparable.compareTo(o.key);
        }
        return key.toString().compareTo(o.key.toString());
    }
}
