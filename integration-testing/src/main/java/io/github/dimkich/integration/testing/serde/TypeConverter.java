package io.github.dimkich.integration.testing.serde;

import io.github.dimkich.integration.testing.format.common.type.TypeParser;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.properties.ConfigurationPropertiesBinding;
import org.springframework.core.convert.converter.Converter;
import org.springframework.util.StringUtils;

import java.lang.reflect.Type;

/**
 * Spring {@link Converter} that parses a textual type reference into a {@link Type} using the
 * configured {@link TypeParser}. Used to bind the {@code targetClass} field of serde configuration
 * properties.
 */
@ConfigurationPropertiesBinding
@RequiredArgsConstructor
public class TypeConverter implements Converter<String, Type> {

    private final ObjectProvider<TypeParser> typeParser;

    @Override
    @SuppressWarnings("NullableProblems")
    public Type convert(String source) {
        return StringUtils.hasText(source) ? typeParser.getObject().parse(source) : null;
    }
}
