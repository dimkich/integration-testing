package io.github.dimkich.integration.testing.format.xml.version;

public interface XmlMigration {
    void migrate(String path) throws Exception;
}
