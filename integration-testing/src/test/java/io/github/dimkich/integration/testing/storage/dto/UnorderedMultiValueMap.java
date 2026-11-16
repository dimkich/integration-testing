package io.github.dimkich.integration.testing.storage.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.github.dimkich.integration.testing.storage.jackson.UnorderedMultiValueMapDeserializer;
import org.springframework.util.MultiValueMapAdapter;

import java.util.HashMap;

/**
 * Multi-value map without an order of its own: the difference engine has to sort its keys.
 */
@JsonDeserialize(using = UnorderedMultiValueMapDeserializer.class)
public class UnorderedMultiValueMap extends MultiValueMapAdapter<String, Object> {

    public UnorderedMultiValueMap() {
        super(new HashMap<>());
    }
}
