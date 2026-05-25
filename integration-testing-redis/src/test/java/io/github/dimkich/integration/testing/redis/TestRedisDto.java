package io.github.dimkich.integration.testing.redis;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class TestRedisDto implements Serializable {
    private String id;
    private Instant createdAt;
    private List<String> tags;
    private Map<String, Integer> metrics;
    private boolean active;
}

