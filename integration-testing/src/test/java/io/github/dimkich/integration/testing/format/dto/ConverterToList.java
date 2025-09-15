package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import lombok.*;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
@NoArgsConstructor
@JsonSerialize(converter = ConverterToList.From.class)
@JsonDeserialize(converter = ConverterToList.To.class)
public class ConverterToList {
    private String firstName;
    private String middleName;
    private String lastName;

    static class From extends StdConverter<ConverterToList, List<String>> {
        @Override
        @SneakyThrows
        public List<String> convert(ConverterToList value) {
            return List.of(value.getFirstName(), value.getMiddleName(), value.getLastName());
        }
    }

    static class To extends StdConverter<List<String>, ConverterToList> {
        @Override
        public ConverterToList convert(List<String> value) {
            return new ConverterToList(value.get(0), value.get(1), value.get(2));
        }
    }
}
