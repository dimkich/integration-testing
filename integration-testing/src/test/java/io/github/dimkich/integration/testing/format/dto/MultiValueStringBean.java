package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.github.dimkich.integration.testing.web.jackson.LinkedMultiValueMapStringString;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@Data
@NoArgsConstructor
public class MultiValueStringBean {
    @JsonSerialize(as = LinkedMultiValueMapStringString.class)
    @JsonDeserialize(as = LinkedMultiValueMapStringString.class)
    private MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();

    private String name = "after";
}
