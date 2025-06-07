package io.github.dimkich.integration.testing.xml;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import io.github.dimkich.integration.testing.Module;
import io.github.dimkich.integration.testing.xml.attributes.BeanAsAttributes;
import io.github.dimkich.integration.testing.xml.map.JsonMapKey;
import io.github.dimkich.integration.testing.xml.token.XmlTokenBuffer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {XmlConfig.class, XmlMapperTest.Config.class})
class XmlMapperTest {
    private final XmlMapper xmlMapper;

    @Autowired
    public XmlMapperTest(XmlTestCaseMapper xmlTestCaseMapper) {
        xmlMapper = (XmlMapper) (xmlTestCaseMapper.unwrap())
                .disable(ToXmlGenerator.Feature.WRITE_XML_1_1)
                .enable(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)
                .disable(SerializationFeature.INDENT_OUTPUT);
    }

    static Object[][] data() {
        return new Object[][]{
                {1, "<Integer>1</Integer>"},
                {"str", "<String>str</String>"},
                {new Typed(new ArrayList<>(List.of(new TypeTest(1, null, "s"),
                        new TypeTest(2, null, "t"))),
                        map(new LinkedHashMap<>(), "k1", new TypeTest(3, null, "d"))),
                        "<Typed><list><id>1</id><name>s</name></list><list><id>2</id><name>t</name></list><map><k1><id>3</id><name>d</name></k1></map></Typed>"},
                {new Typed(new ArrayList<>(List.of(new TypeTest(1, null, "s"))), null),
                        "<Typed><list><id>1</id><name>s</name></list></Typed>"},
                {new ArrayList<>(List.of("1", "2", "3")),
                        "<ArrayList><item type=\"string\">1</item><item type=\"string\">2</item><item type=\"string\">3</item></ArrayList>"},
                {new TypeTest(1, map(new LinkedHashMap<>(), "k1", 1, "k2", "s"), "s"),
                        "<TypeTest><id>1</id><data type=\"linkedHashMap\"><k1 type=\"integer\">1</k1><k2 type=\"string\">s</k2></data><name>s</name></TypeTest>"},
                {new TypeTest(1, new ArrayList<>(List.of("1", 2, 3.0)), "s"),
                        "<TypeTest><id>1</id><data type=\"arrayList\"><data type=\"string\">1</data><data type=\"integer\">2</data><data type=\"double\">3.0</data></data><name>s</name></TypeTest>"},
                {new BeanAsAttr(new Attr().put("id", "1").put("data", "d"), "s", 12),
                        "<BeanAsAttr id=\"1\" data=\"d\"><name>s</name><id>12</id></BeanAsAttr>"},
                {new TypeTest(1, map(new MapKeyWrapped<>(), "k1", 1, "k2", "s"), "s"),
                        "<TypeTest><id>1</id><data type=\"mapKeyWrapped\"><entry><key type=\"string\">k1</key><value type=\"integer\">1</value></entry><entry><key type=\"string\">k2</key><value type=\"string\">s</value></entry></data><name>s</name></TypeTest>"},
                {new TypeTest(1, map(new MapAttrWrapped<>(), "k1", 1, "k2", "s"), "s"),
                        "<TypeTest><id>1</id><data type=\"mapAttrWrapped\"><entry key=\"k1\" utype=\"integer\">1</entry><entry key=\"k2\" utype=\"string\">s</entry></data><name>s</name></TypeTest>"},
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    void serialize(Object o, String xml) throws JsonProcessingException {
        assertEquals(xml, xmlMapper.writeValueAsString(o));
    }

    @ParameterizedTest
    @MethodSource("data")
    void deserialize(Object o, String xml) throws JsonProcessingException {
        assertEquals(o, xmlMapper.readValue(xml, o.getClass()));
    }

    @ParameterizedTest
    @MethodSource("data")
    void deserializeTokens(Object o, String xml) throws IOException {
        if (o instanceof BeanAsAttr) {
            return;
        }
        if (o instanceof TypeTest t && (t.getData() instanceof MapKeyWrapped || t.data instanceof MapAttrWrapped)) {
            return;
        }
        JsonParser p = xmlMapper.createParser(xml);
        p.nextToken();
        XmlTokenBuffer buffer = new XmlTokenBuffer(p, null);
        buffer.copyCurrentStructure(p);
        p = buffer.asParser();
        p.nextToken();
        assertEquals(o, p.readValueAs(o.getClass()));
    }

    @Configuration
    static class Config {
        @Bean
        Module testModule() {
            return new Module().addSubTypes(MapKeyNotWrapped.class, MapKeyWrapped.class,
                    MapAttrNotWrapped.class, MapAttrWrapped.class, TypeTest.class);
        }
    }

    @Data
    @AllArgsConstructor
    static class TypeTest {
        private Integer id;
        private Object data;
        private String name;
    }


    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Typed {
        private List<TypeTest> list = new ArrayList<>();
        private Map<String, TypeTest> map = new LinkedHashMap<>();
    }

    @JsonMapKey(JsonMapKey.Type.AS_KEY_TAG)
    public static class MapKeyNotWrapped<K, V> extends LinkedHashMap<K, V> {
    }

    @JsonMapKey(value = JsonMapKey.Type.AS_KEY_TAG, wrapped = true)
    public static class MapKeyWrapped<K, V> extends LinkedHashMap<K, V> {
    }

    @JsonMapKey(JsonMapKey.Type.AS_ATTRIBUTE)
    public static class MapAttrNotWrapped<K, V> extends LinkedHashMap<K, V> {
    }

    @JsonMapKey(value = JsonMapKey.Type.AS_ATTRIBUTE, wrapped = true)
    public static class MapAttrWrapped<K, V> extends LinkedHashMap<K, V> {
    }

    @Data
    @AllArgsConstructor
    public static class BeanAsAttr {
        @BeanAsAttributes
        private Attr attr;
        private String name;
        private Integer id;
    }

    @Data
    @NoArgsConstructor
    public static class Attr {
        @Getter(onMethod_ = @JsonAnyGetter)
        private Map<String, Object> map = new LinkedHashMap<>();

        @JsonAnySetter
        public Attr put(String name, Object value) {
            map.put(name, value);
            return this;
        }
    }


    static <K, V> Map<K, V> map(Map<K, V> map, Object... keyValues) {
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((K) keyValues[i], (V) keyValues[i + 1]);
        }
        return map;
    }
}