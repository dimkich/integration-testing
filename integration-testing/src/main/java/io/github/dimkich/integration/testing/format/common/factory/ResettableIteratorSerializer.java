package io.github.dimkich.integration.testing.format.common.factory;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.impl.IteratorSerializer;

import java.io.IOException;
import java.util.Iterator;

/**
 * Custom Jackson serializer for iterators that can be rewound.
 * <p>
 * If the provided iterator implements {@link ResettableIterator}, the iterator is reset
 * before and after serialization so repeated serialization passes observe the same data.
 */
public class ResettableIteratorSerializer extends IteratorSerializer {
    /**
     * Creates a serializer for iterator elements.
     *
     * @param elemType     element type of the iterator
     * @param staticTyping whether static typing is enabled
     * @param vts          optional type serializer for elements
     */
    public ResettableIteratorSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts) {
        super(elemType, staticTyping, vts);
    }

    /**
     * Copy constructor used by Jackson when contextualizing this serializer.
     *
     * @param src             source serializer
     * @param property        current bean property
     * @param vts             optional type serializer for elements
     * @param valueSerializer optional explicit element serializer
     * @param unwrapSingle    whether single-element containers should be unwrapped
     */
    public ResettableIteratorSerializer(IteratorSerializer src, BeanProperty property, TypeSerializer vts, JsonSerializer<?> valueSerializer, Boolean unwrapSingle) {
        super(src, property, vts, valueSerializer, unwrapSingle);
    }

    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new ResettableIteratorSerializer(this, _property, vts, _elementSerializer, _unwrapSingle);
    }

    @Override
    public IteratorSerializer withResolved(BeanProperty property,
                                           TypeSerializer vts, JsonSerializer<?> elementSerializer,
                                           Boolean unwrapSingle) {
        return new ResettableIteratorSerializer(this, property, vts, elementSerializer, unwrapSingle);
    }

    /**
     * Resets rewindable iterators before and after Jackson consumes them.
     *
     * @param value    iterator to serialize
     * @param g        JSON generator
     * @param provider serializer provider
     * @throws IOException if serialization fails
     */
    @Override
    public void serializeContents(Iterator<?> value, JsonGenerator g, SerializerProvider provider) throws IOException {
        if (value instanceof ResettableIterator<?> iterator) {
            iterator.reset();
        }
        super.serializeContents(value, g, provider);
        if (value instanceof ResettableIterator<?> iterator) {
            iterator.reset();
        }
    }
}
