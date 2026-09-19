package io.github.dimkich.integration.testing.kafka.serde.json;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JsonTestDto {
    private String id;
    private String name;
    private int amount;
    private double price;
    private boolean active;
    private List<String> tags;
    private Instant timestamp;
    private LocalDateTime localDateTime;
}
