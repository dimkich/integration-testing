package io.github.dimkich.integration.testing.initialization.sql;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import io.github.dimkich.integration.testing.initialization.TestInit;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;
import java.util.Set;

@Getter
@Setter
@ToString
public class SqlStorageSetup extends TestInit {
    @JacksonXmlProperty(isAttribute = true)
    private String name;
    private List<String> sqlFilePath;
    private List<String> sql;
    private Set<String> dbUnitPath;
    private Set<TableHook> tableHook;

    @Data
    public static class TableHook {
        @JacksonXmlProperty(isAttribute = true)
        private String tableName;
        @JacksonXmlProperty(isAttribute = true)
        private String beanName;
        @JacksonXmlProperty(isAttribute = true)
        private String beanMethod;
    }
}
