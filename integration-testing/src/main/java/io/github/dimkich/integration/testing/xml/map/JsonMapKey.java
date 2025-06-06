package io.github.dimkich.integration.testing.xml.map;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE, ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonMapKey {
    @RequiredArgsConstructor
    @Getter
    enum Type {
        AS_ATTRIBUTE(MapEntryKeyAsAttribute.class), AS_KEY_TAG(MapEntryKeyAsTag.class);
        private final Class<? extends MapEntry> cls;
    }

    Type value();

    boolean wrapped() default false;
}
