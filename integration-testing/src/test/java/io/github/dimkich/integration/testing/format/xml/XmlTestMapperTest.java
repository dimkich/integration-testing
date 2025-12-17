package io.github.dimkich.integration.testing.format.xml;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.TestContainer;
import io.github.dimkich.integration.testing.TestPart;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.format.FormatTestUtils;
import io.github.dimkich.integration.testing.format.dto.*;
import io.github.dimkich.integration.testing.format.xml.attributes.BeanAsAttributes;
import io.github.dimkich.integration.testing.format.xml.map.JsonMapKey;
import io.github.dimkich.integration.testing.storage.mapping.Container;
import io.github.dimkich.integration.testing.storage.mapping.EntryStringKeyObjectValue;
import io.github.dimkich.integration.testing.web.WebConfig;
import io.github.dimkich.integration.testing.web.jackson.LinkedMultiValueMapStringObject;
import io.github.dimkich.integration.testing.web.jackson.LinkedMultiValueMapStringString;
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
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.URI;
import java.security.SecureRandom;
import java.util.*;

import static io.github.dimkich.integration.testing.format.FormatTestUtils.compConfig;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {XmlConfig.class, WebConfig.class, XmlTestMapperTest.Config.class})
class XmlTestMapperTest {

    private final XmlMapper xmlMapper;

    @Autowired
    public XmlTestMapperTest(XmlTestMapper xmlTestMapper) {
        xmlMapper = (XmlMapper) (xmlTestMapper.unwrap()).disable(ToXmlGenerator.Feature.WRITE_XML_1_1);
    }

    static Object[][] data() {
        return new Object[][]{
                {1, "\n<Integer>1</Integer>"},
                {"str", "\n<String>str</String>"},
                {new Value(null, null), """
<Value/>
"""},
                {new Value(null, "str"), """
<Value>
    <value>str</value>
</Value>
"""},
                {new Value(null, ""), """
<Value>
    <value></value>
</Value>
"""},
                {new Value(null, " "), """
<Value>
    <value> </value>
</Value>
"""},
                {new Value((byte) 12), """
<Value>
    <value type="byte">12</value>
</Value>
"""},
                {new Value((short) 8), """
<Value>
    <value type="short">8</value>
</Value>
"""},
                {new Value(12), """
<Value>
    <value type="integer">12</value>
</Value>
"""},
                {new Value((long) 45), """
<Value>
    <value type="long">45</value>
</Value>
"""},
                {new Value(2.4), """
<Value>
    <value type="double">2.4</value>
</Value>
"""},
                {new Value((float) 1.22), """
<Value>
    <value type="float">1.22</value>
</Value>
"""},
                {new Value((float) 1.22), """
<Value>
    <value type="float">1.22</value>
</Value>
"""},
                {new Value(true), """
<Value>
    <value type="boolean">true</value>
</Value>
"""},
                {new Value(false), """
<Value>
    <value type="boolean">false</value>
</Value>
"""},
                {new Value('2'), """
<Value>
    <value type="character">2</value>
</Value>
"""},
                {new Value(' '), """
<Value>
    <value type="character"> </value>
</Value>
"""},
                {new Value(new BigDecimal("1.230000")), """
<Value>
    <value type="bigDecimal">1.23</value>
</Value>
"""},
                {new Value(new byte[]{1, 2, 3}), """
<Value>
    <value type="byte[]">AQID</value>
</Value>
"""},
                {new Value(new ByteArrayResource(new byte[]{3, 2, 1})), """
<Value>
    <value type="resource">AwIB</value>
</Value>
"""},
                {new Value(new SecureRandom()), """
<Value>
    <value type="secureRandom"></value>
</Value>
"""},
                {new Value(HttpMethod.GET), """
<Value>
    <value type="httpMethod">GET</value>
</Value>
"""},
                {new Value(new LinkedMultiValueMapStringString(Map.of("k1", List.of("v1")))), """
<Value>
    <value type="linkedMultiValueMapStringString">
        <k1>v1</k1>
    </value>
</Value>
"""},
                {new Value(new LinkedMultiValueMapStringObject(Map.of("k1", List.of(1.2f)))), """
<Value>
    <value type="linkedMultiValueMapStringObject">
        <k1 type="float">1.2</k1>
    </value>
</Value>
"""},
                {new Value(FormatTestUtils.sr2(new byte[]{1, 2, 3})), """
<Value>
    <value type="resource">AQID</value>
</Value>
"""},
                {new Value(Arrays.asList(
                        null, List.of(),
                        List.of(List.of(1L, 2), Arrays.asList("s", null, false, null)),
                        List.of(List.of(5.0, 6.0f), Arrays.asList(null, 7, 8)))), """
<Value>
    <value type="arrayList">
        <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
        <value type="arrayList"/>
        <value type="arrayList">
            <item type="arrayList">
                <item type="long">1</item>
                <item type="integer">2</item>
            </item>
            <item type="arrayList">
                <item>s</item>
                <item xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                <item type="boolean">false</item>
                <item xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
            </item>
        </value>
        <value type="arrayList">
            <item type="arrayList">
                <item type="double">5.0</item>
                <item type="float">6.0</item>
            </item>
            <item type="arrayList">
                <item xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                <item type="integer">7</item>
                <item type="integer">8</item>
            </item>
        </value>
    </value>
</Value>
"""},
                {new Value(new Object[]{null, List.of(), true, "s", new int[]{1, 2}}), """
<Value>
    <value type="object[]">
        <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
        <value type="arrayList"/>
        <value type="boolean">true</value>
        <value>s</value>
        <value type="int[]">
            <item>1</item>
            <item>2</item>
        </value>
    </value>
</Value>
"""},
                {new Value(new Value("a", 1)), """
<Value>
    <value type="value" attr="a">
        <value type="integer">1</value>
    </value>
</Value>
"""},
                {new ListOfListOfListOfInt(Collections.singletonList(null)),
                        """
<root>
    <data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
</root>
"""},
                {new ListOfListOfListOfInt(Arrays.asList(null, null)),
                        """
<root>
    <data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    <data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
</root>
"""},
                {new ListOfListOfListOfInt(Arrays.asList(null, List.of())),
                        """
<root>
    <data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    <data/>
</root>
"""},
                {new ListOfListOfListOfInt(List.of(
                        List.of(List.of(1, 2), List.of(3, 4)),
                        List.of(List.of(5, 6), List.of(7, 8)))),
                        """
<root>
    <data>
        <data>
            <data>1</data>
            <data>2</data>
        </data>
        <data>
            <data>3</data>
            <data>4</data>
        </data>
    </data>
    <data>
        <data>
            <data>5</data>
            <data>6</data>
        </data>
        <data>
            <data>7</data>
            <data>8</data>
        </data>
    </data>
</root>
"""},
                {new ListOfListOfObject(Arrays.asList(null, Arrays.asList(null, List.of(), null))), """
<root>
    <data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    <data>
        <data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
        <data type="arrayList"/>
        <data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    </data>
</root>
"""},
                {new ListOfListOfObject(List.of(List.of(false, 2), List.of((byte) 3,
                        new TypeTest(1, true, null)))), """
<root>
    <data>
        <data type="boolean">false</data>
        <data type="integer">2</data>
    </data>
    <data>
        <data type="byte">3</data>
        <data type="typeTest">
            <id>1</id>
            <data type="boolean">true</data>
        </data>
    </data>
</root>
"""},
                {new EntryStringKeyObjectValue("k", Container.ChangeType.added, "str"), """
<EntryStringKeyObjectValue key="k" change="added" utype="string">str</EntryStringKeyObjectValue>
"""},
                {HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                        FormatTestUtils.httpHeaders("Expires", List.of("0"), "Custom", List.of("c", "a")),
                        new byte[]{}, null), """
<BadRequest>
    <statusCode>BAD_REQUEST</statusCode>
    <rawStatusCode>400</rawStatusCode>
    <responseHeaders>
        <Expires>0</Expires>
        <Custom>c</Custom>
        <Custom>a</Custom>
    </responseHeaders>
    <message>400 Bad Request</message>
</BadRequest>
"""},
                {new RequestEntity<>("str", FormatTestUtils.httpHeaders("Expires", List.of("0"),
                        "Custom", List.of("c", "a")), HttpMethod.POST, URI.create("/api")), """
<RequestEntity>
    <url>/api</url>
    <method>POST</method>
    <headers>
        <Expires>0</Expires>
        <Custom>c</Custom>
        <Custom>a</Custom>
    </headers>
    <body>str</body>
</RequestEntity>
"""},
                {new RequestEntity<>(FormatTestUtils.sr2(new byte[]{1}), new HttpHeaders(), HttpMethod.POST,
                        URI.create("/api")), """
<RequestEntity>
    <url>/api</url>
    <method>POST</method>
    <body type="resource">AQ==</body>
</RequestEntity>
"""},
                {new LinkedMultiValueMapStringString(FormatTestUtils.map("k1", List.of("v1"), "k2", List.of("v2"))),
                        """
<LinkedMultiValueMapStringString>
    <k1>v1</k1>
    <k2>v2</k2>
</LinkedMultiValueMapStringString>
"""},
                {new LinkedMultiValueMapStringObject(Map.of("k", List.of(1L, true))),
                        """
<LinkedMultiValueMapStringObject>
    <k type="long">1</k>
    <k type="boolean">true</k>
</LinkedMultiValueMapStringObject>
"""},
                {new LinkedMultiValueMapStringObject(FormatTestUtils.map("k1", List.of("v1"), "k2", List.of("v2"))),
                        """
<LinkedMultiValueMapStringObject>
    <k1>v1</k1>
    <k2>v2</k2>
</LinkedMultiValueMapStringObject>
"""},
                {new TestContainer(), "<test type=\"container\"/>\n"},
                {new TestCase(), "<test type=\"case\"/>\n"},
                {new TestPart(), "<test type=\"part\"/>\n"},
                {new Value(new ConverterToList("1", "2", "3")), """
<Value>
    <value type="converterToList">
        <value>1</value>
        <value>2</value>
        <value>3</value>
    </value>
</Value>
"""},
                {new ConverterOnField("all", "a,b,c, "), """
<root>
    <name>all</name>
    <list>a</list>
    <list>b</list>
    <list>c</list>
    <list> </list>
</root>
"""},
                {new IncludeAlways(null, null, null, null), """
<root>
    <id xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    <code xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    <attributes xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    <map xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
</root>
"""},
                {new IncludeAlways(1, "c", Collections.singletonList(null), Map.of("a", 1)), """
<root>
    <id>1</id>
    <code>c</code>
    <attributes>
        <attributes xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    </attributes>
    <map>
        <a type="integer">1</a>
    </map>
</root>
"""},

                {new Typed(new ArrayList<>(List.of(new TypeTest(1, null, "s"),
                        new TypeTest(2, null, "t"))),
                        map(new LinkedHashMap<>(), "k1", new TypeTest(3, null, "d"))), """
<Typed>
    <list>
        <id>1</id>
        <name>s</name>
    </list>
    <list>
        <id>2</id>
        <name>t</name>
    </list>
    <map>
        <k1>
            <id>3</id>
            <name>d</name>
        </k1>
    </map>
</Typed>
"""},
                {new Typed(new ArrayList<>(List.of(new TypeTest(1, null, "s"))), null), """
<Typed>
    <list>
        <id>1</id>
        <name>s</name>
    </list>
</Typed>
"""},
                {new ArrayList<>(List.of("1", "2", "3")), """
<ArrayList>
    <item>1</item>
    <item>2</item>
    <item>3</item>
</ArrayList>
"""},
                {new TypeTest(1, map(new LinkedHashMap<>(), "k1", 1, "k2", "s"), "s"), """
<TypeTest>
    <id>1</id>
    <data type="linkedHashMap">
        <k1 type="integer">1</k1>
        <k2>s</k2>
    </data>
    <name>s</name>
</TypeTest>
"""},
                {new TypeTest(1, new ArrayList<>(List.of("1", 2, 3.0)), "s"), """
<TypeTest>
    <id>1</id>
    <data type="arrayList">
        <data>1</data>
        <data type="integer">2</data>
        <data type="double">3.0</data>
    </data>
    <name>s</name>
</TypeTest>
"""},
                {new BeanAsAttr(new Attr().put("id", "1").put("data", "d"), "s", 12), """
<BeanAsAttr id="1" data="d">
    <name>s</name>
    <id>12</id>
</BeanAsAttr>
"""},
                {new TypeTest(1, map(new MapKeyWrapped<>(), "k1", 1, "k2", "s"), "s"), """
<TypeTest>
    <id>1</id>
    <data type="mapKeyWrapped">
        <entry>
            <key>k1</key>
            <value type="integer">1</value>
        </entry>
        <entry>
            <key>k2</key>
            <value>s</value>
        </entry>
    </data>
    <name>s</name>
</TypeTest>
"""},
                {new TypeTest(null, null, ""), """
<TypeTest>
    <name></name>
</TypeTest>
"""},
                {new TypeTest(null, null, " "), """
<TypeTest>
    <name> </name>
</TypeTest>
"""},
                {new TypeTest(1, map(new MapAttrWrapped<>(), "k1", 1, "k2", "s"), "s"), """
<TypeTest>
    <id>1</id>
    <data type="mapAttrWrapped">
        <entry key="k1" utype="integer">1</entry>
        <entry key="k2" utype="string">s</entry>
    </data>
    <name>s</name>
</TypeTest>
"""},
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    void serialize(Object o, String xml) throws JsonProcessingException {
        Class<?> cls = o.getClass();
        if (o instanceof io.github.dimkich.integration.testing.Test) {
            cls = io.github.dimkich.integration.testing.Test.class;
        }
        assertEquals(xml.replaceAll("\n", System.lineSeparator()),
                xmlMapper.writerFor(cls).writeValueAsString(o));
    }

    @ParameterizedTest
    @MethodSource("data")
    void deserialize(Object o, String xml) throws JsonProcessingException {
        assertThat(o).usingRecursiveComparison(compConfig).isEqualTo(xmlMapper.readValue(xml, o.getClass()));
    }

    @ParameterizedTest
    @MethodSource("data")
    void deserializeTokens(Object o, String xml) throws IOException {
        if (o instanceof BeanAsAttr) {
            return;
        }
        JsonParser p = xmlMapper.createParser(xml);
        p.nextToken();
        TokenBuffer buffer = xmlMapper.getDeserializationContext().bufferForInputBuffering(p);
        buffer.copyCurrentStructure(p);
        p = buffer.asParserOnFirstToken();
        assertThat(o).usingRecursiveComparison(compConfig).isEqualTo(p.readValueAs(o.getClass()));
    }

    @Configuration
    static class Config {
        @Bean
        TestSetupModule testModule() {
            return new TestSetupModule().addSubTypes(MapKeyNotWrapped.class, MapKeyWrapped.class,
                    MapAttrNotWrapped.class, MapAttrWrapped.class, TypeTest.class, Value.class,
                    ConverterToList.class);
        }
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