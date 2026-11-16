package io.github.dimkich.integration.testing.redis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Hash field key written as a structure. The key type is structured, so the difference keeps its
 * fields visible by default instead of joining them into a line of text.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RedisComplexKey implements Serializable {
    private String exchange;
    private String classCode;
    private String ticker;
}
