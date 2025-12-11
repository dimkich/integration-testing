package io.github.dimkich.integration.testing.initialization.sql;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
public class SqlStorageInit extends TestInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    @JacksonXmlProperty(isAttribute = true)
    private Boolean loadAllTables;
    @JacksonXmlProperty(isAttribute = true)
    private Boolean disableTableHooks;
    private String tablesToChange;
    private String tablesToLoad;
    private List<String> sql;
}