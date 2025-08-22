package io.github.dimkich.integration.testing.format.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.dimkich.integration.testing.format.FormatTestUtils;
import io.github.dimkich.integration.testing.format.dto.Value;
import io.github.dimkich.integration.testing.storage.mapping.Container;
import io.github.dimkich.integration.testing.storage.mapping.EntryStringKeyObjectValue;
import io.github.dimkich.integration.testing.web.WebConfig;
import io.github.dimkich.integration.testing.web.jackson.LinkedMultiValueMapStringObject;
import io.github.dimkich.integration.testing.web.jackson.LinkedMultiValueMapStringString;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.web.client.HttpClientErrorException;

import java.math.BigDecimal;
import java.net.URI;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

import static io.github.dimkich.integration.testing.format.FormatTestUtils.compConfig;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = {JsonConfig.class, WebConfig.class})
class JsonTestMapperTest {
    private final JsonMapper jsonMapper;

    @Autowired
    public JsonTestMapperTest(JsonTestMapper jsonTestMapper) {
        jsonMapper = (JsonMapper) (jsonTestMapper.unwrap());
        jsonMapper.disable(SerializationFeature.INDENT_OUTPUT);
    }

    static Object[][] data() {
        return new Object[][]{
                {1, "1"},
                {"str", "\"str\""},
                {new Value("str"), "{\"value\":\"str\"}"},
                {new Value((byte) 12), "{\"value\":[\"byte\",12]}"},
                {new Value((short) 8), "{\"value\":[\"short\",8]}"},
                {new Value(12), "{\"value\":12}"},
                {new Value((long) 45), "{\"value\":[\"long\",45]}"},
                {new Value(2.4), "{\"value\":2.4}"},
                {new Value((float) 1.22), "{\"value\":[\"float\",1.22]}"},
                {new Value((float) 1.22), "{\"value\":[\"float\",1.22]}"},
                {new Value(true), "{\"value\":true}"},
                {new Value(false), "{\"value\":false}"},
                {new Value('2'), "{\"value\":[\"character\",\"2\"]}"},
                {new Value(new BigDecimal("1.230000")), "{\"value\":[\"bigDecimal\",\"1.23\"]}"},
                {new Value(new byte[]{1, 2, 3}), "{\"value\":[\"byte[]\",\"AQID\"]}"},
                {new Value(new ByteArrayResource(new byte[]{3, 2, 1})), "{\"value\":[\"resource\",\"AwIB\"]}"},
                {new Value(new SecureRandom()), "{\"value\":[\"secureRandom\",\"\"]}"},
                {new Value(HttpMethod.GET), "{\"value\":[\"httpMethod\",\"GET\"]}"},
                {new Value(new LinkedMultiValueMapStringString(Map.of("k1", List.of("v1")))),
                        "{\"value\":{\"type\":\"linkedMultiValueMapStringString\",\"k1\":[\"v1\"]}}"},
                {new Value(new LinkedMultiValueMapStringObject(Map.of("k1", List.of(1.2f)))),
                        "{\"value\":{\"type\":\"linkedMultiValueMapStringObject\",\"k1\":[[\"float\",1.2]]}}"},
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
                {new LinkedMultiValueMapStringString(FormatTestUtils.map("k1", List.of("v1"), "k2", List.of("v2"))),
                        "{\"k1\":[\"v1\"],\"k2\":[\"v2\"]}"},
                {new LinkedMultiValueMapStringObject(Map.of("k", List.of(1L, true))), "{\"k\":[[\"long\",1],true]}"},
                {new LinkedMultiValueMapStringObject(FormatTestUtils.map("k1", List.of("v1"), "k2", List.of("v2"))),
                        "{\"k1\":[\"v1\"],\"k2\":[\"v2\"]}"},
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    void serialize(Object o, String json) throws JsonProcessingException {
        assertEquals(json, jsonMapper.writeValueAsString(o));
    }

    @ParameterizedTest
    @MethodSource("data")
    void deserialize(Object o, String json) throws JsonProcessingException {
        assertThat(o).usingRecursiveComparison(compConfig).isEqualTo(jsonMapper.readValue(json, o.getClass()));
    }
}