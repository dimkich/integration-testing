package io.github.dimkich.integration.testing.xml;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.NameTransformer;
import com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser;
import io.github.dimkich.integration.testing.util.SupplierWithIO;

import javax.xml.stream.Location;
import java.io.IOException;

public class StoreLocationSerializer extends DelegatingDeserializer {
    private final ObjectToLocationStorage objectToLocationStorage;

    public StoreLocationSerializer(JsonDeserializer<?> d, ObjectToLocationStorage objectToLocationStorage) {
        super(d);
        this.objectToLocationStorage = objectToLocationStorage;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        return deserialize(p, () -> super.deserialize(p, ctxt));
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt, Object intoValue) throws IOException {
        return deserialize(p, () -> super.deserialize(p, ctxt, intoValue));
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer) throws IOException {
        return deserialize(p, () -> super.deserializeWithType(p, ctxt, typeDeserializer));
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext ctxt, TypeDeserializer typeDeserializer, Object intoValue) throws IOException {
        return deserialize(p, () -> super.deserializeWithType(p, ctxt, typeDeserializer, intoValue));
    }

    private Object deserialize(JsonParser p, SupplierWithIO<Object> objectSupplier) throws IOException {
        Location location = null;
        if (p instanceof FromXmlParser parser) {
            location = parser.getStaxReader().getLocation();
        }
        Object object = objectSupplier.get();
        objectToLocationStorage.put(object, location);
        return object;
    }

    @Override
    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper) {
        JsonDeserializer<?> unwrapped = _delegatee.unwrappingDeserializer(unwrapper);
        if (unwrapped != _delegatee) {
            return (JsonDeserializer<Object>) newDelegatingInstance(unwrapped);
        }
        return this;
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new StoreLocationSerializer(newDelegatee, objectToLocationStorage);
    }
}
