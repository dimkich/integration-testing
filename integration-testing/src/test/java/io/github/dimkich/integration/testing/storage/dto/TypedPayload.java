package io.github.dimkich.integration.testing.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.LinkedHashMap;

/**
 * Value whose presenter exposes the payload itself and asks the context for the payload type, so
 * the difference element keeps the type of the presented value rather than the wrapper type.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TypedPayload {
    private LinkedHashMap<String, Object> payload;
}
