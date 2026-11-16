package io.github.dimkich.integration.testing.format.xml;

/**
 * Parser that can tell an XML attribute from a child element.
 * <p>
 * The XML parser reports both attributes and child elements as fields, so a deserializer that
 * needs the difference asks the parser. The capability survives buffering: {@code XmlTokenBuffer}
 * marks attributes while copying and its parser keeps the marks, so a deserializer works the same
 * on the original XML stream and on a buffered one.
 */
public interface AttributeAwareParser {

    /**
     * @return {@code true} when the current field name came from an XML attribute
     */
    boolean isCurrentTokenAttribute();
}
