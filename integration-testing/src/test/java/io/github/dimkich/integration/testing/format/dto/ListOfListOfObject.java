package io.github.dimkich.integration.testing.format.dto;

import com.fasterxml.jackson.annotation.JsonRootName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonRootName("root")
public class ListOfListOfObject {
    private List<List<Object>> data;
}
