package io.github.dimkich.integration.testing.format.common.type;

import io.github.dimkich.integration.testing.format.common.CommonFormatConfig;
import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticGenericArrayType;
import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticParameterizedType;
import io.github.dimkich.integration.testing.format.common.type.synthetic.SyntheticWildcardType;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Slf4j
@SpringBootTest(classes = {CommonFormatConfig.class})
class TypeParserTest {
    @Autowired
    private TypeParser typeParser;

    public static Object[][] data() {
        return new Object[][]{
                {"ArrayList", ArrayList.class, null},
                {"Object[]", new SyntheticGenericArrayType(Object.class), null},
                {"Integer[]", new SyntheticGenericArrayType(Integer.class), null},
                {"Object[][]", new SyntheticGenericArrayType(new SyntheticGenericArrayType(Object.class)), null},
                {"ArrayList <String>", new SyntheticParameterizedType(ArrayList.class, new Type[]{String.class}), null},
                {"ArrayList< ArrayList <Long>>", new SyntheticParameterizedType(ArrayList.class, new Type[]{
                        new SyntheticParameterizedType(ArrayList.class, new Type[]{Long.class})}), null},
                {"LinkedHashMap<byte[], int[]>", new SyntheticParameterizedType(LinkedHashMap.class,
                        new Type[]{new SyntheticGenericArrayType(byte.class),
                                new SyntheticGenericArrayType(int.class)}), null},
                {"LinkedHashMap<Integer , ArrayList<String>>",
                        new SyntheticParameterizedType(LinkedHashMap.class, new Type[]{
                                Integer.class,
                                new SyntheticParameterizedType(ArrayList.class, new Type[]{String.class})
                        }), null},
                {"LinkedHashMap< ? extends Integer , ArrayList<? super String>>",
                        new SyntheticParameterizedType(LinkedHashMap.class, new Type[]{
                                new SyntheticWildcardType(new Type[]{Integer.class}, new Type[]{}),
                                new SyntheticParameterizedType(ArrayList.class, new Type[]{
                                        new SyntheticWildcardType(new Type[]{Object.class}, new Type[]{String.class})
                                })
                        }), null},
                {"ArrayList<?>", new SyntheticParameterizedType(ArrayList.class, new Type[]{
                        new SyntheticWildcardType(new Type[]{Object.class}, new Type[]{})
                }), null},
                {"java.util.LinkedHashMap<java.lang.Integer, java.util.ArrayList<java.lang.String>>",
                        new SyntheticParameterizedType(LinkedHashMap.class, new Type[]{
                                Integer.class,
                                new SyntheticParameterizedType(ArrayList.class, new Type[]{String.class})
                        }), null},
                {"ArrayList<String>[]", new SyntheticGenericArrayType(
                        new SyntheticParameterizedType(ArrayList.class, new Type[]{String.class})
                ), null},
                {"java.lang.LinkedHashMap", null, "java.lang.LinkedHashMap"},
                {"ArrayList<>", null, "Expecting type name in 'ArrayList<>'"},
                {"ArrayList>", null, "Too much '>' in 'ArrayList>'"},
                {"ArrayList<String?>", null, "Unexpected token QUESTION in 'ArrayList<String?>'"},
                {"ArrayList?", null, "Unexpected token QUESTION in 'ArrayList?'"},
                {"ArrayList<? extends ,", null, "Wrong wildcard format in 'ArrayList<? extends ,'"},
                {"ArrayList<,>", null, "Expecting type name in 'ArrayList<,>'"},
                {"ArrayList<? e String>", null, "Expected 'extends' or 'super' after '?' in 'ArrayList<? e String>', got 'e'"},
                {"ArrayList<long>>", null, "Too much '>' in 'ArrayList<long>>'"},
                {"ArrayList<byte[]><", null, "Incorrect format in 'ArrayList<byte[]><'"},
        };
    }

    @ParameterizedTest
    @MethodSource("data")
    void parse(String typeDesc, Type type, String error) {
        try {
            assertEquals(type, typeParser.parse(typeDesc));
        } catch (Throwable e) {
            log.error("", e);
            assertEquals(error, e.getMessage());
        }
    }
}