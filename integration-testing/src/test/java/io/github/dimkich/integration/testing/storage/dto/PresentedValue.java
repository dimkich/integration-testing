package io.github.dimkich.integration.testing.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Value whose presentation is defined by the test data: the body and the optional {@code ttl} and
 * {@code score} attributes. The body is rendered by the usual rules, so one fixture covers scalar,
 * map, collection, array and multi-value map bodies. {@code orderedAttributes} makes the presenter
 * return a {@link java.util.LinkedHashMap}, so the tests can check that an ordered map keeps its
 * own order instead of being sorted by name.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PresentedValue {
    private Object body;
    private String ttl;
    private String score;
    private boolean orderedAttributes;
}
