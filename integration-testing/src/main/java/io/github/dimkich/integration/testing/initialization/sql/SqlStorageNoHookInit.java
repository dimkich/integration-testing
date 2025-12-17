package io.github.dimkich.integration.testing.initialization.sql;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SqlStorageNoHookInit extends TestInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    private List<String> sql;
}
