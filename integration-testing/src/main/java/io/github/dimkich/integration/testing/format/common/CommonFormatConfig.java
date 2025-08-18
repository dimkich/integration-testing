package io.github.dimkich.integration.testing.format.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.format.common.mixin.*;
import io.github.dimkich.integration.testing.format.common.serializer.BigDecimalSerializer;
import io.github.dimkich.integration.testing.openapi.FieldErrorMixIn;
import io.github.dimkich.integration.testing.openapi.SpringErrorDto;
import io.github.sugarcubes.cloner.CopyAction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.validation.FieldError;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.security.SecureRandom;

@Configuration
@ConditionalOnClass(ObjectMapper.class)
public class CommonFormatConfig {
    @Bean
    TestSetupModule commonFormatTestSetupModule() {
        return new TestSetupModule()
                .addSubTypes(byte[].class, "byte[]")
                .addAlias(ByteArrayResource.class, "resource")
                .addSubTypes(SecureRandom.class, SpringErrorDto.class, Resource.class)
                .clonerTypeAction(Throwable.class::isAssignableFrom, CopyAction.ORIGINAL)
                .clonerTypeAction(SecureRandom.class, CopyAction.ORIGINAL)
                .clonerTypeAction(ByteArrayInputStream.class, CopyAction.ORIGINAL)
                .clonerTypeAction(Resource.class::isAssignableFrom, CopyAction.ORIGINAL)
                .addEqualsForType(SecureRandom.class, (sr1, sr2) -> true)
                .addJacksonModule(new SimpleModule().addSerializer(BigDecimal.class, new BigDecimalSerializer())
                        .setMixInAnnotation(Throwable.class, ThrowableMixIn.class)
                        .setMixInAnnotation(FieldError.class, FieldErrorMixIn.class)
                        .setMixInAnnotation(FieldError.class, FieldErrorMixIn.class)
                        .setMixInAnnotation(SecureRandom.class, SecureRandomMixIn.class)
                        .setMixInAnnotation(Resource.class, SpringResourceMixIn.class)
                        .setMixInAnnotation(byte[].class, ByteArrayMixIn.class))
                .addJacksonModule(new JavaTimeModule());
    }
}
