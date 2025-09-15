package io.github.dimkich.integration.testing.format.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TypeTest {
    private Integer id;
    private Object data;
    private String name;
}
