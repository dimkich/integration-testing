package io.github.dimkich.integration.testing.format.xml.fixed;

import com.fasterxml.jackson.databind.deser.BeanDeserializerFactory;
import com.fasterxml.jackson.dataformat.xml.JacksonXmlModule;
import com.fasterxml.jackson.dataformat.xml.XmlFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;

public class XmlMapperFixed extends XmlMapper {
    public XmlMapperFixed(XmlFactory xmlFactory) {
        this(xmlFactory, DEFAULT_XML_MODULE);
    }

    public XmlMapperFixed(XmlFactory xmlFactory, JacksonXmlModule module) {
        super(xmlFactory, module);
        _deserializationContext = new XmlDeserializationContextFixed(BeanDeserializerFactory.instance);
    }
}
