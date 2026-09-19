package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.dimkich.integration.testing.web.jackson.LinkedMultiValueMapStringObject;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Data
@NoArgsConstructor
public class MultiValueBean {
    @JsonSerialize(as = LinkedMultiValueMapStringObject.class)
    @JsonDeserialize(as = LinkedMultiValueMapStringObject.class)
    private MultiValueMap<String, Object> headers = new LinkedMultiValueMap<>();

    private String name = "after";
}