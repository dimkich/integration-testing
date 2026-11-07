package io.github.dimkich.integration.testing.postgresql;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO used by the PostgreSQL serde integration test.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostgresSerdeDto {
    private int id;
    private String name;
}
