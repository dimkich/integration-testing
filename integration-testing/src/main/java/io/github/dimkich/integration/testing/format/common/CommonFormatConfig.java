package io.github.dimkich.integration.testing.format.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.github.dimkich.integration.testing.*;
import io.github.dimkich.integration.testing.format.common.location.StoreLocationModule;
import io.github.dimkich.integration.testing.format.common.mixin.ByteArrayInputStreamMixIn;
import io.github.dimkich.integration.testing.format.common.mixin.SecureRandomMixIn;
import io.github.dimkich.integration.testing.format.common.mixin.SpringResourceMixIn;
import io.github.dimkich.integration.testing.format.common.mixin.ThrowableMixIn;
import io.github.dimkich.integration.testing.format.common.serializer.BigDecimalSerializer;
import io.github.dimkich.integration.testing.openapi.FieldErrorMixIn;
import io.github.dimkich.integration.testing.openapi.SpringErrorDto;
import io.github.sugarcubes.cloner.CopyAction;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.validation.FieldError;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.*;

@Configuration
@ConditionalOnClass(ObjectMapper.class)
@Import(ObjectMapperConfigurer.class)
public class CommonFormatConfig {
    @Bean
    TestSetupModule commonFormatTestSetupModule() throws ClassNotFoundException {
        return new TestSetupModule()
                .addParentType(Object.class).addParentType(Throwable.class).addParentType(Test.class)
                .addSubTypes(TestContainer.class, "container")
                .addSubTypes(TestCase.class, "case")
                .addSubTypes(TestPart.class, "part")
                .addSubTypes(byte[].class, "byte[]")
                .addSubTypes(UUID.class, "UUID")
                .addAlias(ByteArrayResource.class, "resource")
                .addAlias(Class.forName("java.util.ImmutableCollections$List12"), "arrayList")
                .addAlias(Class.forName("java.util.ImmutableCollections$ListN"), "arrayList")
                .addAlias(Class.forName("java.util.Collections$SingletonList"), "arrayList")
                .addAlias(Class.forName("java.util.Collections$UnmodifiableMap"), "linkedHashMap")
                .addSubTypes(String.class, Character.class, Long.class, Integer.class, Short.class, Byte.class,
                        Double.class, Float.class, BigDecimal.class, BigInteger.class, Boolean.class, ArrayList.class,
                        LinkedHashMap.class, TreeMap.class, LinkedHashSet.class, TreeSet.class, Class.class, Date.class,
                        LocalTime.class, LocalDate.class, LocalDateTime.class, ZonedDateTime.class,
                        SecureRandom.class, SpringErrorDto.class, Resource.class, ByteArrayInputStream.class)
                .clonerTypeAction(Throwable.class::isAssignableFrom, CopyAction.ORIGINAL)
                .clonerTypeAction(SecureRandom.class, CopyAction.ORIGINAL)
                .clonerTypeAction(ByteArrayInputStream.class, CopyAction.ORIGINAL)
                .clonerTypeAction(Resource.class::isAssignableFrom, CopyAction.ORIGINAL)
                .addEqualsForType(SecureRandom.class, (sr1, sr2) -> true)
                .addEqualsForType(ByteArrayInputStream.class, (o1, o2) -> {
                    byte[] b1 = o1.readAllBytes();
                    o1.reset();
                    byte[] b2 = o2.readAllBytes();
                    o2.reset();
                    return Arrays.equals(b1, b2);
                })
                .addJacksonModule(new SimpleModule().addSerializer(BigDecimal.class, new BigDecimalSerializer())
                        .setMixInAnnotation(Throwable.class, ThrowableMixIn.class)
                        .setMixInAnnotation(FieldError.class, FieldErrorMixIn.class)
                        .setMixInAnnotation(FieldError.class, FieldErrorMixIn.class)
                        .setMixInAnnotation(SecureRandom.class, SecureRandomMixIn.class)
                        .setMixInAnnotation(Resource.class, SpringResourceMixIn.class)
                        .setMixInAnnotation(ByteArrayInputStream.class, ByteArrayInputStreamMixIn.class))
                .addJacksonModule(new JavaTimeModule())
                .addJacksonModule(new StoreLocationModule());
    }
}
