package io.github.dimkich.integration.testing.format.xml.wrapper;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerModifier;
import com.fasterxml.jackson.dataformat.xml.util.AnnotationUtil;
import com.fasterxml.jackson.dataformat.xml.util.TypeUtil;
import io.github.dimkich.integration.testing.format.xml.fixed.XmlBeanPropertyWriterFixed;

import java.util.List;

public class WrappedSerializerModifier extends BeanSerializerModifier {

    @Override
    public List<BeanPropertyWriter> changeProperties(SerializationConfig config, BeanDescription beanDesc, List<BeanPropertyWriter> beanProperties) {
        AnnotationIntrospector intr = config.getAnnotationIntrospector();
        for (int i = 0; i < beanProperties.size(); i++) {
            BeanPropertyWriter bpw = beanProperties.get(i);
            if (!TypeUtil.isIndexedType(bpw.getType())) {
                continue;
            }
            AnnotatedMember member = bpw.getMember();
            JsonInclude include = member.getAnnotation(JsonInclude.class);
            if (include == null || include.value() != JsonInclude.Include.ALWAYS) {
                continue;
            }
            String ns = AnnotationUtil.findNamespaceAnnotation(config, intr, member);
            PropertyName wrappedName = PropertyName.construct(bpw.getName(), ns);
            PropertyName wrapperName = bpw.getWrapperName();
            if (wrapperName == null || wrapperName == PropertyName.NO_NAME) {
                wrapperName = wrappedName;
            }
            beanProperties.set(i, new XmlBeanPropertyWriterFixed(bpw, wrapperName, wrappedName));
        }
        return beanProperties;
    }
}
