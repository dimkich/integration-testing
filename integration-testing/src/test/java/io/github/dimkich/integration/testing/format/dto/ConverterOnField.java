package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JacksonXmlRootElement(localName = "root")
public class ConverterOnField {
    private String name;
    @JsonSerialize(converter = ConverterOnField.From.class)
    @JsonDeserialize(converter = ConverterOnField.To.class)
    private String list;

    static class From extends StdConverter<String, List<String>> {
        @Override
        @SneakyThrows
        public List<String> convert(String value) {
            return Arrays.asList(value.split(","));
        }
    }

    static class To extends StdConverter<List<String>, String> {
        @Override
        public String convert(List<String> value) {
            return String.join(",", value);
        }
    }
}
