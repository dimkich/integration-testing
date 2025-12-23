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
import io.github.dimkich.integration.testing.format.common.map.LinkedHashMapObjectObject;
import io.github.dimkich.integration.testing.format.common.map.LinkedHashMapStringObject;
import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticGenericArrayType;
import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticParameterizedType;
import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticWildcardType;
import io.github.dimkich.integration.testing.format.dto.*;
import io.github.dimkich.integration.testing.format.xml.attributes.BeanAsAttributes;
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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.io.IOException;
import java.lang.reflect.Type;
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
    <value type="Byte">12</value>
</Value>
"""},
                {new Value((short) 8), """
<Value>
    <value type="Short">8</value>
</Value>
"""},
                {new Value(12), """
<Value>
    <value type="Integer">12</value>
</Value>
"""},
                {new Value((long) 45), """
<Value>
    <value type="Long">45</value>
</Value>
"""},
                {new Value(2.4), """
<Value>
    <value type="Double">2.4</value>
</Value>
"""},
                {new Value((float) 1.22), """
<Value>
    <value type="Float">1.22</value>
</Value>
"""},
                {new Value((float) 1.22), """
<Value>
    <value type="Float">1.22</value>
</Value>
"""},
                {new Value(true), """
<Value>
    <value type="Boolean">true</value>
</Value>
"""},
                {new Value(false), """
<Value>
    <value type="Boolean">false</value>
</Value>
"""},
                {new Value('2'), """
<Value>
    <value type="Character">2</value>
</Value>
"""},
                {new Value(' '), """
<Value>
    <value type="Character"> </value>
</Value>
"""},
                {new Value(new BigDecimal("1.230000")), """
<Value>
    <value type="BigDecimal">1.23</value>
</Value>
"""},
                {new Value(new byte[]{1, 2, 3}), """
<Value>
    <value type="byte[]">AQID</value>
</Value>
"""},
                {new Value(new ByteArrayResource(new byte[]{3, 2, 1})), """
<Value>
    <value type="Resource">AwIB</value>
</Value>
"""},
                {new Value(new SecureRandom()), """
<Value>
    <value type="SecureRandom"></value>
</Value>
"""},
                {new Value(HttpMethod.GET), """
<Value>
    <value type="HttpMethod">GET</value>
</Value>
"""},
                {new Value(new LinkedMultiValueMapStringString(Map.of("k1", List.of("v1")))), """
<Value>
    <value type="LinkedMultiValueMapStringString">
        <k1>v1</k1>
    </value>
</Value>
"""},
                {new Value(new LinkedMultiValueMapStringObject(Map.of("k1", List.of(1.2f)))), """
<Value>
    <value type="LinkedMultiValueMapStringObject">
        <k1 type="Float">1.2</k1>
    </value>
</Value>
"""},
                {new Value(FormatTestUtils.sr2(new byte[]{1, 2, 3})), """
<Value>
    <value type="Resource">AQID</value>
</Value>
"""},
                {new Value(Arrays.asList(
                        null, List.of(),
                        List.of(List.of(1L, 2), Arrays.asList("s", null, false, null)),
                        List.of(List.of(5.0, 6.0f), Arrays.asList(null, 7, 8)))), """
<Value>
    <value type="ArrayList">
        <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
        <value type="ArrayList"/>
        <value type="ArrayList">
            <item type="ArrayList">
                <item type="Long">1</item>
                <item type="Integer">2</item>
            </item>
            <item type="ArrayList">
                <item>s</item>
                <item xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                <item type="Boolean">false</item>
                <item xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
            </item>
        </value>
        <value type="ArrayList">
            <item type="ArrayList">
                <item type="Double">5.0</item>
                <item type="Float">6.0</item>
            </item>
            <item type="ArrayList">
                <item xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
                <item type="Integer">7</item>
                <item type="Integer">8</item>
            </item>
        </value>
    </value>
</Value>
"""},
                {new Value(new Object[]{null, List.of(), true, "s", new int[]{1, 2}}), """
<Value>
    <value type="Object[]">
        <value xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
        <value type="ArrayList"/>
        <value type="Boolean">true</value>
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
    <value type="Value" attr="a">
        <value type="Integer">1</value>
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
        <data type="ArrayList"/>
        <data xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:nil="true"/>
    </data>
</root>
"""},
                {new ListOfListOfObject(List.of(List.of(false, 2), List.of((byte) 3,
                        new TypeTest(1, true, null)))), """
<root>
    <data>
        <data type="Boolean">false</data>
        <data type="Integer">2</data>
    </data>
    <data>
        <data type="Byte">3</data>
        <data type="TypeTest">
            <id>1</id>
            <data type="Boolean">true</data>
        </data>
    </data>
</root>
"""},
                {new EntryStringKeyObjectValue("k", Container.ChangeType.added, "str"), """
<EntryStringKeyObjectValue key="k" change="added" utype="String">str</EntryStringKeyObjectValue>
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
    <body type="Resource">AQ==</body>
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
    <k type="Long">1</k>
    <k type="Boolean">true</k>
</LinkedMultiValueMapStringObject>
"""},
                {new LinkedMultiValueMapStringObject(FormatTestUtils.map("k1", List.of("v1"), "k2", List.of("v2"))),
                        """
<LinkedMultiValueMapStringObject>
    <k1>v1</k1>
    <k2>v2</k2>
</LinkedMultiValueMapStringObject>
"""},
                {new TestContainer(), "<test type=\"Container\"/>\n"},
                {new TestCase(), "<test type=\"Case\"/>\n"},
                {new TestPart(), "<test type=\"Part\"/>\n"},
                {new Value(new ConverterToList("1", "2", "3")), """
<Value>
    <value type="ConverterToList">
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
        <a type="Integer">1</a>
    </map>
</root>
"""},
                {map(new LinkedHashMapObjectObject<>(), new TypeTest(1, null, "1"), new Value("a", 12L),
                        new TypeTest(2, null, "2"), null), """
<LinkedHashMapObjectObject>
    <entry>
        <key type="TypeTest">
            <id>1</id>
            <name>1</name>
        </key>
        <value type="Value" attr="a">
            <value type="Long">12</value>
        </value>
    </entry>
    <entry>
        <key type="TypeTest">
            <id>2</id>
            <name>2</name>
        </key>
    </entry>
</LinkedHashMapObjectObject>
"""},
                {new TypeTest(1, map(new LinkedHashMapObjectObject<>(), "k1", 1, "k2", "s"), "s"), """
<TypeTest>
    <id>1</id>
    <data type="LinkedHashMapObjectObject">
        <entry>
            <key>k1</key>
            <value type="Integer">1</value>
        </entry>
        <entry>
            <key>k2</key>
            <value>s</value>
        </entry>
    </data>
    <name>s</name>
</TypeTest>
"""},
                {new TypeTest(1, map(new LinkedHashMapStringObject<>(), "k1", 1, "k2", "s", "k3", null), "s"), """
<TypeTest>
    <id>1</id>
    <data type="LinkedHashMapStringObject">
        <entry key="k1" utype="Integer">1</entry>
        <entry key="k2" utype="String">s</entry>
        <entry key="k3"/>
    </data>
    <name>s</name>
</TypeTest>
"""},
                {new TypeTest(1, map(new MapAttrNotWrapped<>(), "k1", 1, "k2", "s", "k3", null), "s"), """
<TypeTest>
    <id>1</id>
    <data type="MapAttrNotWrapped">
        <data key="k1" utype="Integer">1</data>
        <data key="k2" utype="String">s</data>
        <data key="k3"/>
    </data>
    <name>s</name>
</TypeTest>
"""},
                {new TypeTest(2, map(new MapElemNotWrapped<>(), new TypeTest(1, null, "1"),
                        new Value("a", 12L), "k1", 1, "k2", "s", "k3", null), "t"), """
<TypeTest>
    <id>2</id>
    <data type="MapElemNotWrapped">
        <data>
            <key type="TypeTest">
                <id>1</id>
                <name>1</name>
            </key>
            <value type="Value" attr="a">
                <value type="Long">12</value>
            </value>
        </data>
        <data>
            <key>k1</key>
            <value type="Integer">1</value>
        </data>
        <data>
            <key>k2</key>
            <value>s</value>
        </data>
        <data>
            <key>k3</key>
        </data>
    </data>
    <name>t</name>
</TypeTest>
"""},
                {new OrderBook(new TreeMap<>(Map.of(BigDecimal.valueOf(1.2), BigDecimal.valueOf(12.3),
                        BigDecimal.valueOf(1.33), BigDecimal.valueOf(16.3))),
                        new TreeMap<>(Map.of(BigDecimal.valueOf(11.2), BigDecimal.valueOf(122.32),
                                BigDecimal.valueOf(13.2), BigDecimal.valueOf(145.94)))), """
<OrderBook>
    <bid key="1.2">12.3</bid>
    <bid key="1.33">16.3</bid>
    <offer key="11.2">122.32</offer>
    <offer key="13.2">145.94</offer>
</OrderBook>
"""},
                {new OrderBookWrapped(new TreeMap<>(Map.of(BigDecimal.valueOf(1.2), BigDecimal.valueOf(12.3),
                        BigDecimal.valueOf(1.33), BigDecimal.valueOf(16.3))),
                        new TreeMap<>(Map.of(BigDecimal.valueOf(11.2), BigDecimal.valueOf(122.32),
                                BigDecimal.valueOf(13.2), BigDecimal.valueOf(145.94)))), """
<OrderBookWrapped>
    <bids>
        <entry key="1.2">12.3</entry>
        <entry key="1.33">16.3</entry>
    </bids>
    <offers>
        <entry key="11.2">122.32</entry>
        <entry key="13.2">145.94</entry>
    </offers>
</OrderBookWrapped>
"""},
                {new Value(ArrayList.class), """
<Value>
    <value type="Class">ArrayList</value>
</Value>
"""},
                {new Value(HashMap.class), """
<Value>
    <value type="Class">java.util.HashMap</value>
</Value>
"""},
                {new Value(new SyntheticParameterizedType(ArrayList.class, new Type[]{String.class})), """
<Value>
    <value type="Type">ArrayList&lt;String></value>
</Value>
"""},
                {new Value(new SyntheticGenericArrayType(new SyntheticGenericArrayType(Object.class))), """
<Value>
    <value type="Type">Object[][]</value>
</Value>
"""},
                {new Value(new SyntheticParameterizedType(LinkedHashMap.class, new Type[]{
                        new SyntheticWildcardType(new Type[]{Integer.class}, new Type[]{}),
                        new SyntheticParameterizedType(ArrayList.class, new Type[]{
                                new SyntheticWildcardType(new Type[]{Object.class}, new Type[]{String.class})
                        })
                })
                ), """
<Value>
    <value type="Type">LinkedHashMap&lt;? extends Integer, ArrayList&lt;? super String>></value>
</Value>
"""},
                {new Value(ParameterizedTypeReference.forType(ArrayList.class)), """
<Value>
    <value type="ParameterizedTypeReference">ArrayList</value>
</Value>
"""},
                {new Value(ParameterizedTypeReference.forType(
                        new SyntheticParameterizedType(ArrayList.class, new Type[]{String.class}))), """
<Value>
    <value type="ParameterizedTypeReference">ArrayList&lt;String></value>
</Value>
"""},
                {new Value(ParameterizedTypeReference.forType(
                        new SyntheticGenericArrayType(new SyntheticGenericArrayType(Object.class)))), """
<Value>
    <value type="ParameterizedTypeReference">Object[][]</value>
</Value>
"""},
                {new Value(ParameterizedTypeReference.forType(
                        new SyntheticParameterizedType(LinkedHashMap.class, new Type[]{
                                new SyntheticWildcardType(new Type[]{Integer.class}, new Type[]{}),
                                new SyntheticParameterizedType(ArrayList.class, new Type[]{
                                        new SyntheticWildcardType(new Type[]{Object.class}, new Type[]{String.class})
                                })
                        })
                )), """
<Value>
    <value type="ParameterizedTypeReference">LinkedHashMap&lt;? extends Integer, ArrayList&lt;? super String>></value>
</Value>
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
    <data type="LinkedHashMap">
        <k1 type="Integer">1</k1>
        <k2>s</k2>
    </data>
    <name>s</name>
</TypeTest>
"""},
                {new TypeTest(1, new ArrayList<>(List.of("1", 2, 3.0)), "s"), """
<TypeTest>
    <id>1</id>
    <data type="ArrayList">
        <data>1</data>
        <data type="Integer">2</data>
        <data type="Double">3.0</data>
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
        assertThat(xmlMapper.readValue(xml, o.getClass())).usingRecursiveComparison(compConfig).isEqualTo(o);
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
            return new TestSetupModule().addSubTypes(MapElemNotWrapped.class, MapAttrNotWrapped.class,
                    TypeTest.class, Value.class, ConverterToList.class);
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class Typed {
        private List<TypeTest> list = new ArrayList<>();
        private Map<String, TypeTest> map = new LinkedHashMap<>();
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


    @SuppressWarnings("unchecked")
    static <K, V> Map<K, V> map(Map<K, V> map, Object... keyValues) {
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((K) keyValues[i], (V) keyValues[i + 1]);
        }
        return map;
    }
}