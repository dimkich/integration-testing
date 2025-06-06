package io.github.dimkich.integration.testing.xml.attributes;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.AnyGetterWriter;

import java.util.HashMap;
import java.util.Map;

public class AnyGetterFinder {
    private final Map<Class<?>, AnyGetterWriter> anyGetterMap = new HashMap<>();

    public AnyGetterWriter find(Class<?> cls, SerializerProvider prov) throws JsonMappingException {
        if (!anyGetterMap.containsKey(cls)) {
            prov.findValueSerializer(cls);
        }
        AnyGetterWriter anyGetterWriter = anyGetterMap.get(cls);
        if (anyGetterWriter == null) {
            throw new RuntimeException(String.format("Class %s don`t have @JsonAnyGetter annotation", cls.getName()));
        }
        return anyGetterWriter;
    }

    public void put(Class<?> cls, AnyGetterWriter anyGetterWriter) {
        anyGetterMap.put(cls, anyGetterWriter);
    }
}
