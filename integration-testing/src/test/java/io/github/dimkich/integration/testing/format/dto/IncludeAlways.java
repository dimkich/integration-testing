package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonRootName("root")
@Getter(onMethod_ = @JsonInclude(value = JsonInclude.Include.ALWAYS))
public class IncludeAlways {
    private Integer id;
    private String code;
    private List<String> attributes;
    private Map<String, Object> map;
}
