package io.github.dimkich.integration.testing.storage.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.ZonedDateTime;

/**
 * Object with a nullable temporal field: a difference between a value and an absent field stays
 * visible.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ZonedHolder {
    private ZonedDateTime value;
}
