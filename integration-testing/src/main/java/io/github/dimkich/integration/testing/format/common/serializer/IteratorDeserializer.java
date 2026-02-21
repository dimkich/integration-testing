package io.github.dimkich.integration.testing.format.common.serializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import io.github.dimkich.integration.testing.format.common.factory.ResettableIterator;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

/**
 * Jackson deserializer for {@link Iterator} values.
 * <p>
 * Incoming JSON arrays are materialized into a {@link List} and then wrapped
 * into a {@link ResettableIterator} so the resulting iterator can be reused.
 */
public class IteratorDeserializer extends StdDeserializer<Iterator<?>> implements ContextualDeserializer {
    /**
     * Creates a deserializer bound to the {@link Iterator} raw type.
     */
    public IteratorDeserializer() {
        super(Iterator.class);
    }

    /**
     * Deserializes a JSON array into a resettable iterator of objects.
     *
     * @param p    parser positioned at the current JSON token
     * @param ctxt deserialization context
     * @return iterator backed by the parsed list
     * @throws IOException if JSON cannot be read
     */
    @Override
    public Iterator<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        List<?> list = ctxt.readValue(p, List.class);
        return new ResettableIterator<>(list);
    }

    /**
     * Resolves element-aware deserialization when iterator generic type is available.
     * <p>
     * If contextual generic type cannot be determined, current instance is reused.
     *
     * @param ctxt     deserialization context
     * @param property bean property being deserialized
     * @return contextual iterator deserializer
     * @throws JsonMappingException if type resolution fails
     */
    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, BeanProperty property) throws JsonMappingException {
        JavaType iteratorType = ctxt.getContextualType() != null
                ? ctxt.getContextualType()
                : (property != null ? property.getType() : null);

        if (iteratorType == null || !iteratorType.hasRawClass(Iterator.class)) {
            return this;
        }
        JavaType elementType = iteratorType.containedType(0);
        if (elementType == null) {
            elementType = ctxt.constructType(Object.class);
        }
        JavaType listType = ctxt.getTypeFactory().constructCollectionType(List.class, elementType);
        JsonDeserializer<Object> listDeserializer = ctxt.findContextualValueDeserializer(listType, property);

        return new LoadedIteratorDeserializer(listDeserializer);
    }

    /**
     * Contextual iterator deserializer that delegates list conversion to a resolved
     * list deserializer preserving iterator element type.
     */
    private static class LoadedIteratorDeserializer extends JsonDeserializer<Iterator<?>> {
        private final JsonDeserializer<Object> listDeserializer;

        /**
         * Creates a contextual iterator deserializer.
         *
         * @param listDeserializer resolved deserializer for list values
         */
        public LoadedIteratorDeserializer(JsonDeserializer<Object> listDeserializer) {
            this.listDeserializer = listDeserializer;
        }

        /**
         * Deserializes using contextual list deserializer and wraps result into a resettable iterator.
         *
         * @param p    parser positioned at the current JSON token
         * @param ctxt deserialization context
         * @return iterator backed by the parsed list
         * @throws IOException if JSON cannot be read
         */
        @Override
        public Iterator<?> deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
            List<?> list = (List<?>) listDeserializer.deserialize(p, ctxt);
            return new ResettableIterator<>(list);
        }

        /**
         * @return handled raw type
         */
        @Override
        public Class<?> handledType() {
            return Iterator.class;
        }
    }
}

