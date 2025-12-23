package io.github.dimkich.integration.testing.format.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
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
import io.github.dimkich.integration.testing.storage.mapping.Container;
import io.github.dimkich.integration.testing.storage.mapping.EntryStringKeyObjectValue;
import io.github.dimkich.integration.testing.web.WebConfig;
import io.github.dimkich.integration.testing.web.jackson.LinkedMultiValueMapStringObject;
import io.github.dimkich.integration.testing.web.jackson.LinkedMultiValueMapStringString;
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

import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.net.URI;
import java.security.SecureRandom;
import java.util.*;

import static io.github.dimkich.integration.testing.format.FormatTestUtils.compConfig;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {JsonConfig.class, WebConfig.class, JsonTestMapperTest.Config.class})
class JsonTestMapperTest {
    private final JsonMapper jsonMapper;

    @Autowired
    public JsonTestMapperTest(JsonTestMapper jsonTestMapper) {
        jsonMapper = jsonTestMapper.unwrap();
        jsonMapper.disable(SerializationFeature.INDENT_OUTPUT);
    }

    static Object[][] data() {
        return new Object[][]{
                {1, "1"},
                {"str", "\"str\""},
                {new Value(null, null), "{}"},
                {new Value(null, "str"), "{\"value\":\"str\"}"},
                {new Value(null, ""), "{\"value\":\"\"}"},
                {new Value(null, " "), "{\"value\":\" \"}"},
                {new Value((byte) 12), "{\"value\":[\"Byte\",12]}"},
                {new Value((short) 8), "{\"value\":[\"Short\",8]}"},
                {new Value(12), "{\"value\":12}"},
                {new Value((long) 45), "{\"value\":[\"Long\",45]}"},
                {new Value(2.4), "{\"value\":2.4}"},
                {new Value((float) 1.22), "{\"value\":[\"Float\",1.22]}"},
                {new Value((float) 1.22), "{\"value\":[\"Float\",1.22]}"},
                {new Value(true), "{\"value\":true}"},
                {new Value(false), "{\"value\":false}"},
                {new Value('2'), "{\"value\":[\"Character\",\"2\"]}"},
                {new Value(' '), "{\"value\":[\"Character\",\" \"]}"},
                {new Value(new BigDecimal("1.230000")), "{\"value\":[\"BigDecimal\",\"1.23\"]}"},
                {new Value(new byte[]{1, 2, 3}), "{\"value\":[\"byte[]\",\"AQID\"]}"},
                {new Value(new ByteArrayResource(new byte[]{3, 2, 1})), "{\"value\":[\"Resource\",\"AwIB\"]}"},
                {new Value(new SecureRandom()), "{\"value\":[\"SecureRandom\",\"\"]}"},
                {new Value(HttpMethod.GET), "{\"value\":[\"HttpMethod\",\"GET\"]}"},
                {new Value(new LinkedMultiValueMapStringString(Map.of("k1", List.of("v1")))),
                        "{\"value\":{\"type\":\"LinkedMultiValueMapStringString\",\"k1\":[\"v1\"]}}"},
                {new Value(new LinkedMultiValueMapStringObject(Map.of("k1", List.of(1.2f)))),
                        "{\"value\":{\"type\":\"LinkedMultiValueMapStringObject\",\"k1\":[[\"Float\",1.2]]}}"},
                {new Value(FormatTestUtils.sr2(new byte[]{1, 2, 3})), "{\"value\":[\"Resource\",\"AQID\"]}"},
                {new Value(Arrays.asList(null, List.of(), List.of(List.of(1L, 2), Arrays.asList("s", null, false, null)),
                        List.of(List.of(5.0, 6.0f), Arrays.asList(null, 7, 8)))),
                        "{\"value\":[\"ArrayList\",[null,[\"ArrayList\",[]],[\"ArrayList\",[[\"ArrayList\",[[\"Long\",1],2]],[\"ArrayList\",[\"s\",null,false,null]]]],[\"ArrayList\",[[\"ArrayList\",[5.0,[\"Float\",6.0]]],[\"ArrayList\",[null,7,8]]]]]]}"},
                {new Value(new Object[]{null, List.of(), true, "s", new int[]{1, 2}}),
                        "{\"value\":[\"Object[]\",[null,[\"ArrayList\",[]],true,\"s\",[\"int[]\",[1,2]]]]}"},
                {new Value(new TestContainer()), "{\"value\":{\"type\":\"Container\"}}"},
                {new Value(new Value("a", 1)), "{\"value\":{\"type\":\"Value\",\"attr\":\"a\",\"value\":1}}"},
                {new ListOfListOfListOfInt(Collections.singletonList(null)), "{\"data\":[null]}"},
                {new ListOfListOfListOfInt(Arrays.asList(null, null)), "{\"data\":[null,null]}"},
                {new ListOfListOfListOfInt(Arrays.asList(null, List.of())), "{\"data\":[null,[]]}"},
                {new ListOfListOfListOfInt(List.of(List.of(List.of(1, 2), List.of(3, 4)), List.of(List.of(5, 6), List.of(7, 8)))),
                        "{\"data\":[[[1,2],[3,4]],[[5,6],[7,8]]]}"},
                {new ListOfListOfObject(Arrays.asList(null, Arrays.asList(null, List.of(), null))),
                        "{\"data\":[null,[null,[\"ArrayList\",[]],null]]}"},
                {new ListOfListOfObject(List.of(List.of(false, 2), List.of((byte) 3,
                        new TypeTest(1, true, null)))),
                        "{\"data\":[[false,2],[[\"Byte\",3],{\"type\":\"TypeTest\",\"id\":1,\"data\":true}]]}"},
                {new EntryStringKeyObjectValue("k", Container.ChangeType.added, "str"),
                        "{\"key\":\"k\",\"change\":\"added\",\"value\":\"str\"}"},
                {HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request",
                        FormatTestUtils.httpHeaders("Expires", List.of("0"), "Custom", List.of("c", "a")),
                        new byte[]{}, null),
                        "{\"statusCode\":\"BAD_REQUEST\",\"rawStatusCode\":400,\"responseHeaders\":{\"Expires\":[\"0\"]," +
                                "\"Custom\":[\"c\",\"a\"]},\"message\":\"400 Bad Request\"}"},
                {new RequestEntity<>("str", FormatTestUtils.httpHeaders("Expires", List.of("0"), "Custom", List.of("c", "a")),
                        HttpMethod.POST, URI.create("/api")), "{\"url\":\"/api\",\"method\":\"POST\"," +
                        "\"headers\":{\"Expires\":[\"0\"],\"Custom\":[\"c\",\"a\"]},\"body\":\"str\"}"},
                {new RequestEntity<>(FormatTestUtils.sr2(new byte[]{1}), new HttpHeaders(), HttpMethod.POST,
                        URI.create("/api")), "{\"url\":\"/api\",\"method\":\"POST\",\"body\":[\"Resource\",\"AQ==\"]}"},
                {new LinkedMultiValueMapStringString(FormatTestUtils.map("k1", List.of("v1"), "k2", List.of("v2"))),
                        "{\"k1\":[\"v1\"],\"k2\":[\"v2\"]}"},
                {new LinkedMultiValueMapStringObject(Map.of("k", List.of(1L, true))), "{\"k\":[[\"Long\",1],true]}"},
                {new LinkedMultiValueMapStringObject(FormatTestUtils.map("k1", List.of("v1"), "k2", List.of("v2"))),
                        "{\"k1\":[\"v1\"],\"k2\":[\"v2\"]}"},
                {new TestContainer(), "{\"type\":\"Container\"}"},
                {new TestCase(), "{\"type\":\"Case\"}"},
                {new TestPart(), "{\"type\":\"Part\"}"},
                {new Value(new ConverterToList("1", "2", "3")),
                        "{\"value\":[\"ConverterToList\",[\"1\",\"2\",\"3\"]]}"},
                {new ConverterOnField("all", "a,b,c, "),
                        "{\"name\":\"all\",\"list\":[\"a\",\"b\",\"c\",\" \"]}"},
                {new IncludeAlways(null, null, null, null),
                        "{\"id\":null,\"code\":null,\"attributes\":null,\"map\":null}"},
                {new IncludeAlways(1, "c", Collections.singletonList(null), Map.of("a", 1)),
                        "{\"id\":1,\"code\":\"c\",\"attributes\":[null],\"map\":{\"a\":1}}"},
                {map(new LinkedHashMapObjectObject<>(), new TypeTest(1, null, "1"), new Value("a", 12L),
                        new TypeTest(2, null, "2"), null),
                        "{\"entry\":[{\"key\":{\"type\":\"TypeTest\",\"id\":1,\"name\":\"1\"},\"value\":{\"type\":\"Value\",\"attr\":\"a\",\"value\":[\"Long\",12]}},{\"key\":{\"type\":\"TypeTest\",\"id\":2,\"name\":\"2\"}}]}"},
                {new TypeTest(1, map(new LinkedHashMapObjectObject<>(), "k1", 1, "k2", "s"), "s"),
                        "{\"id\":1,\"data\":{\"type\":\"LinkedHashMapObjectObject\",\"entry\":[{\"key\":\"k1\",\"value\":1},{\"key\":\"k2\",\"value\":\"s\"}]},\"name\":\"s\"}"},
                {new TypeTest(1, map(new LinkedHashMapStringObject<>(), "k1", 1, "k2", "s", "k3", null), ""),
                        "{\"id\":1,\"data\":{\"type\":\"LinkedHashMapStringObject\",\"entry\":[{\"key\":\"k1\",\"value\":1},{\"key\":\"k2\",\"value\":\"s\"},{\"key\":\"k3\"}]},\"name\":\"\"}"},
                {new TypeTest(1, map(new MapAttrNotWrapped<>(), "k1", 1, "k2", "s", "k3", null), "s"),
                        "{\"id\":1,\"data\":[\"MapAttrNotWrapped\",[{\"key\":\"k1\",\"value\":1},{\"key\":\"k2\",\"value\":\"s\"},{\"key\":\"k3\"}]],\"name\":\"s\"}"},
                {new TypeTest(2, map(new MapElemNotWrapped<>(), new TypeTest(1, null, "1"),
                        new Value("a", 12L), "k1", 1, "k2", "s", "k3", null), "t"),
                        "{\"id\":2,\"data\":[\"MapElemNotWrapped\",[{\"key\":{\"type\":\"TypeTest\",\"id\":1,\"name\":\"1\"},\"value\":{\"type\":\"Value\",\"attr\":\"a\",\"value\":[\"Long\",12]}},{\"key\":\"k1\",\"value\":1},{\"key\":\"k2\",\"value\":\"s\"},{\"key\":\"k3\"}]],\"name\":\"t\"}"},
                {new OrderBook(new TreeMap<>(Map.of(BigDecimal.valueOf(1.2), BigDecimal.valueOf(12.3),
                        BigDecimal.valueOf(1.33), BigDecimal.valueOf(16.3))),
                        new TreeMap<>(Map.of(BigDecimal.valueOf(11.2), BigDecimal.valueOf(122.32),
                                BigDecimal.valueOf(13.2), BigDecimal.valueOf(145.94)))),
                        "{\"bid\":[{\"key\":\"1.2\",\"value\":\"12.3\"},{\"key\":\"1.33\",\"value\":\"16.3\"}],\"offer\":[{\"key\":\"11.2\",\"value\":\"122.32\"},{\"key\":\"13.2\",\"value\":\"145.94\"}]}"},
                {new OrderBookWrapped(new TreeMap<>(Map.of(BigDecimal.valueOf(1.2), BigDecimal.valueOf(12.3),
                        BigDecimal.valueOf(1.33), BigDecimal.valueOf(16.3))),
                        new TreeMap<>(Map.of(BigDecimal.valueOf(11.2), BigDecimal.valueOf(122.32),
                                BigDecimal.valueOf(13.2), BigDecimal.valueOf(145.94)))),
                        "{\"bids\":{\"entry\":[{\"key\":\"1.2\",\"value\":\"12.3\"},{\"key\":\"1.33\",\"value\":\"16.3\"}]},\"offers\":{\"entry\":[{\"key\":\"11.2\",\"value\":\"122.32\"},{\"key\":\"13.2\",\"value\":\"145.94\"}]}}"},
                {new Value(ArrayList.class), "{\"value\":[\"Class\",\"ArrayList\"]}"},
                {new Value(HashMap.class), "{\"value\":[\"Class\",\"java.util.HashMap\"]}"},
                {new Value(new SyntheticParameterizedType(ArrayList.class, new Type[]{String.class})),
                        "{\"value\":[\"Type\",\"ArrayList<String>\"]}"},
                {new Value(new SyntheticGenericArrayType(new SyntheticGenericArrayType(Object.class))),
                        "{\"value\":[\"Type\",\"Object[][]\"]}"},
                {new Value(new SyntheticParameterizedType(LinkedHashMap.class, new Type[]{
                        new SyntheticWildcardType(new Type[]{Integer.class}, new Type[]{}),
                        new SyntheticParameterizedType(ArrayList.class, new Type[]{
                                new SyntheticWildcardType(new Type[]{Object.class}, new Type[]{String.class})
                        })
                })
                ),
                        "{\"value\":[\"Type\",\"LinkedHashMap<? extends Integer, ArrayList<? super String>>\"]}"},
                {new Value(ParameterizedTypeReference.forType(ArrayList.class)),
                        "{\"value\":[\"ParameterizedTypeReference\",\"ArrayList\"]}"},
                {new Value(ParameterizedTypeReference.forType(
                        new SyntheticParameterizedType(ArrayList.class, new Type[]{String.class}))),
                        "{\"value\":[\"ParameterizedTypeReference\",\"ArrayList<String>\"]}"},
                {new Value(ParameterizedTypeReference.forType(
                        new SyntheticGenericArrayType(new SyntheticGenericArrayType(Object.class)))),
                        "{\"value\":[\"ParameterizedTypeReference\",\"Object[][]\"]}"},
                {new Value(ParameterizedTypeReference.forType(
                        new SyntheticParameterizedType(LinkedHashMap.class, new Type[]{
                                new SyntheticWildcardType(new Type[]{Integer.class}, new Type[]{}),
                                new SyntheticParameterizedType(ArrayList.class, new Type[]{
                                        new SyntheticWildcardType(new Type[]{Object.class}, new Type[]{String.class})
                                })
                        })
                )), "{\"value\":[\"ParameterizedTypeReference\",\"LinkedHashMap<? extends Integer, ArrayList<? super String>>\"]}"},
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    void serialize(Object o, String json) throws JsonProcessingException {
        Class<?> cls = o.getClass();
        if (o instanceof io.github.dimkich.integration.testing.Test) {
            cls = io.github.dimkich.integration.testing.Test.class;
        }
        assertEquals(json, jsonMapper.writerFor(cls).writeValueAsString(o));
    }

    @ParameterizedTest
    @MethodSource("data")
    void deserialize(Object o, String json) throws JsonProcessingException {
        assertThat(o).usingRecursiveComparison(compConfig).isEqualTo(jsonMapper.readValue(json, o.getClass()));
    }

    @Configuration
    static class Config {
        @Bean
        TestSetupModule testModule() {
            return new TestSetupModule().addSubTypes(MapElemNotWrapped.class, MapAttrNotWrapped.class,
                    TypeTest.class, Value.class, ConverterToList.class);
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