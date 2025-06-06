package io.github.dimkich.integration.testing.xml.attributes;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.databind.util.NameTransformer;
import lombok.SneakyThrows;

import java.util.List;

public class BeanAsAttributesSerializerModifier extends BeanSerializerModifier {
    private final AnyGetterFinder anyGetterFinder = new AnyGetterFinder();
    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        for (int i = 0; i < beanProperties.size(); i++) {
            BeanPropertyWriter property = beanProperties.get(i);
            BeanAsAttributes beanAsAttributes = property.getAnnotation(BeanAsAttributes.class);
            if (beanAsAttributes != null && beanAsAttributes.enabled()) {
                beanProperties.set(i, new BeanAsAttributesPropertyWriter(property, NameTransformer.NOP, anyGetterFinder));
            }
        }
        return beanProperties;
    }

    @Override
    @SneakyThrows
    public BeanSerializerBuilder updateBuilder(SerializationConfig config, BeanDescription beanDesc, BeanSerializerBuilder builder) {
        anyGetterFinder.put(beanDesc.getBeanClass(), builder.getAnyGetter());
        return builder;
    }
}
