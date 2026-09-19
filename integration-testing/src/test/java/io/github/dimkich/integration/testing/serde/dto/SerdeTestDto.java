package io.github.dimkich.integration.testing.serde.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SerdeTestDto {
    private int id;
    private String name;
    private List<String> items;
}
