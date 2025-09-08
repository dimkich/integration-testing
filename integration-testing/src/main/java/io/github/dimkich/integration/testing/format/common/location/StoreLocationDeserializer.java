package io.github.dimkich.integration.testing.format.common.location;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.std.DelegatingDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.util.NameTransformer;
import eu.ciechanowiec.sneakyfun.SneakySupplier;
import io.github.dimkich.integration.testing.Test;

import java.io.IOException;

public class StoreLocationDeserializer extends DelegatingDeserializer {

    public StoreLocationDeserializer(JsonDeserializer<?> d) {
        super(d);
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

    private Object deserialize(JsonParser p, SneakySupplier<Object, IOException> objectSupplier) throws IOException {
        JsonLocation location = p.currentLocation();
        Object object = objectSupplier.get();
        if (location != null) {
            Test test = (Test) object;
            test.setLineNumber(location.getLineNr());
            test.setColumnNumber(location.getColumnNr());
        }
        return object;
    }

    @Override
    @SuppressWarnings("unchecked")
    public JsonDeserializer<Object> unwrappingDeserializer(NameTransformer unwrapper) {
        JsonDeserializer<?> unwrapped = _delegatee.unwrappingDeserializer(unwrapper);
        if (unwrapped != _delegatee) {
            return (JsonDeserializer<Object>) newDelegatingInstance(unwrapped);
        }
        return this;
    }

    @Override
    protected JsonDeserializer<?> newDelegatingInstance(JsonDeserializer<?> newDelegatee) {
        return new StoreLocationDeserializer(newDelegatee);
    }
}
