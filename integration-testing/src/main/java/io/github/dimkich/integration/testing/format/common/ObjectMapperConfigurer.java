package io.github.dimkich.integration.testing.format.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.cfg.SerializerFactoryConfig;
import com.fasterxml.jackson.databind.ser.BasicSerializerFactory;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;
import io.github.dimkich.integration.testing.TestSetupModule;
import io.github.dimkich.integration.testing.format.common.factory.FixedBeanSerializerFactory;
import lombok.RequiredArgsConstructor;

import java.util.List;

@RequiredArgsConstructor
public class ObjectMapperConfigurer {
    private final List<TestSetupModule> modules;

    public <O extends ObjectMapper, B extends MapperBuilder<O, B>> void configure(MapperBuilder<O, B> builder) {
        SimpleFilterProvider filterProvider = new SimpleFilterProvider();
        for (TestSetupModule module : modules) {
            module.getJacksonModules().forEach(builder::addModules);
            module.getJacksonFilters().forEach(f -> filterProvider.addFilter(f.getKey(), f.getValue()));
            if (module.getHandlerInstantiator() != null) {
                builder.handlerInstantiator(module.getHandlerInstantiator());
            }
        }
        builder.filterProvider(filterProvider);
    }

    public void configure(ObjectMapper mapper) {
        SerializerFactoryConfig config = ((BasicSerializerFactory) mapper.getSerializerFactory()).getFactoryConfig();
        mapper.setSerializerFactory(new FixedBeanSerializerFactory(config));
    }
}
