package io.github.dimkich.integration.testing.format.util;

import com.fasterxml.jackson.databind.JavaType;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class MapTypes {
    private final JavaType keyType;
    private final JavaType valueType;
}
