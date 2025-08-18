package io.github.dimkich.integration.testing.format.xml.polymorphic;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.type.TypeBindings;
import io.github.dimkich.integration.testing.format.xml.map.TypeWithBindings;

public class BeanPropertyWriterAsObject extends BeanPropertyWriter {
    private final JavaType type = new TypeWithBindings(Object.class, TypeBindings.emptyBindings());

    public BeanPropertyWriterAsObject(BeanPropertyWriter base) {
        super(base);
    }

    @Override
    public JavaType getType() {
        return type;
    }
}
