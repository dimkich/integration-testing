package io.github.dimkich.integration.testing.format.common.polymorphic.unwrapped;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.github.dimkich.integration.testing.format.xml.map.MapEntryKeyAsAttribute;
import io.github.dimkich.integration.testing.message.MessageDto;
import io.github.dimkich.integration.testing.storage.mapping.EntryStringKeyObjectValue;
import lombok.Data;

public class DisablePolymorphicUnwrappedModule extends SimpleModule {
    public DisablePolymorphicUnwrappedModule() {
        super();
        setMixInAnnotation(MessageDto.class, MessageDtoMixIn.class);
        setMixInAnnotation(MapEntryKeyAsAttribute.class, MapEntryKeyAsAttributeMixIn.class);
        setMixInAnnotation(EntryStringKeyObjectValue.class, EntryStringKeyObjectValueMixIn.class);
    }

    @Data
    public static class MessageDtoMixIn {
        @JsonUnwrapped(enabled = false)
        private Object payload;
    }

    @Data
    public static class MapEntryKeyAsAttributeMixIn {
        @JsonUnwrapped(enabled = false)
        private Object value;
    }

    @Data
    public static class EntryStringKeyObjectValueMixIn {
        @JsonUnwrapped(enabled = false)
        private Object value;
    }
}
