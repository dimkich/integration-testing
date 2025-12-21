package io.github.dimkich.integration.testing.format.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import io.github.dimkich.integration.testing.format.common.CommonFormatConfig;
import io.github.dimkich.integration.testing.format.common.ObjectMapperConfigurer;
import io.github.dimkich.integration.testing.format.common.polymorphic.unwrapped.DisablePolymorphicUnwrappedModule;
import io.github.dimkich.integration.testing.format.common.type.TestTypeResolverBuilder;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;

@Configuration
@ConditionalOnClass(ObjectMapper.class)
@Import({CommonFormatConfig.class, TestTypeResolverBuilder.class})
public class JsonConfig {
    @Setter(onMethod = @__({@Autowired, @Lazy, @Qualifier("testTypeResolverBuilder")}))
    private TestTypeResolverBuilder resolverBuilder;

    @Bean
    @Lazy
    JsonTestMapper testMapper(ObjectMapperConfigurer configurer) {
        JsonMapper.Builder builder = JsonMapper.builder();
        builder.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        builder.configure(SerializationFeature.WRITE_DURATIONS_AS_TIMESTAMPS, false);
        builder.enable(SerializationFeature.INDENT_OUTPUT);
        builder.disable(SerializationFeature.FAIL_ON_UNWRAPPED_TYPE_IDENTIFIERS);
        builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        builder.enable(JsonParser.Feature.ALLOW_COMMENTS);
        builder.setDefaultTyping(resolverBuilder);
        builder.defaultPrettyPrinter(new CustomPrettyPrinter());

        builder.addModules(new DisablePolymorphicUnwrappedModule());

        configurer.configure(builder);
        JsonMapper jsonMapper = builder.build();
        configurer.configure(jsonMapper);

        return new JsonTestMapper(jsonMapper);
    }
}
