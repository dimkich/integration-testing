package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.util.NameTransformer;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class MapFixedSerializer extends MapSerializer {
    private boolean isUnwrapped;

    public MapFixedSerializer(MapSerializer src) {
        super(src, null, false);
    }

    public MapFixedSerializer(MapSerializer src, BeanProperty property, JsonSerializer<?> keySerializer,
                              JsonSerializer<?> valueSerializer, Set<String> ignoredEntries, Set<String> includedEntries) {
        super(src, property, keySerializer, valueSerializer, ignoredEntries, includedEntries);
    }

    public MapFixedSerializer(MapSerializer src, TypeSerializer vts, Object suppressableValue, boolean suppressNulls) {
        super(src, vts, suppressableValue, suppressNulls);
    }

    public MapFixedSerializer(MapSerializer src, Object filterId, boolean sortKeys) {
        super(src, filterId, sortKeys);
    }

    @Override
    public void serialize(Map<?, ?> value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        if (isUnwrapped) {
            serializeWithoutTypeInfo(value, gen, provider);
            return;
        }
        super.serialize(value, gen, provider);
    }

    @Override
    public void serializeWithType(Map<?, ?> value, JsonGenerator gen, SerializerProvider provider, TypeSerializer typeSer) throws IOException {
        if (isUnwrapped) {
            serializeWithoutTypeInfo(value, gen, provider);
            return;
        }
        super.serializeWithType(value, gen, provider, typeSer);
    }

    @Override
    public JsonSerializer<Map<?, ?>> unwrappingSerializer(NameTransformer unwrapper) {
        if (isUnwrapped) {
            return this;
        }
        MapFixedSerializer mapFixedSerializer = new MapFixedSerializer(this, _filterId, _sortKeys);
        mapFixedSerializer.isUnwrapped = true;
        return mapFixedSerializer;
    }

    @Override
    public boolean isUnwrappingSerializer() {
        return isUnwrapped;
    }

    @Override
    public MapFixedSerializer _withValueTypeSerializer(TypeSerializer vts) {
        if (_valueTypeSerializer == vts) {
            return this;
        }
        return new MapFixedSerializer(this, vts, _suppressableValue, _suppressNulls);
    }

    @Override
    public MapFixedSerializer withResolved(BeanProperty property, JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer, Set<String> ignored, Set<String> included, boolean sortKeys) {
        MapFixedSerializer ser = new MapFixedSerializer(this, property, keySerializer, valueSerializer, ignored, included);
        if (sortKeys != ser._sortKeys) {
            ser = new MapFixedSerializer(ser, _filterId, sortKeys);
        }
        return ser;
    }

    @Override
    public MapFixedSerializer withFilterId(Object filterId) {
        if (_filterId == filterId) {
            return this;
        }
        _ensureOverride("withFilterId");
        return new MapFixedSerializer(this, filterId, _sortKeys);
    }

    @Override
    public MapFixedSerializer withContentInclusion(Object suppressableValue, boolean suppressNulls) {
        if ((suppressableValue == _suppressableValue) && (suppressNulls == _suppressNulls)) {
            return this;
        }
        return new MapFixedSerializer(this, _valueTypeSerializer, suppressableValue, suppressNulls);
    }
}
