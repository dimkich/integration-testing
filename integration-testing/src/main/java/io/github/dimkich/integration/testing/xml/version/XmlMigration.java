package io.github.dimkich.integration.testing.xml.version;

public interface XmlMigration {
    void migrate(String path) throws Exception;
}
