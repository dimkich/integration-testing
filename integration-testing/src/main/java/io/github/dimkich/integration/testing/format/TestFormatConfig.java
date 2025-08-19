package io.github.dimkich.integration.testing.format;

import io.github.dimkich.integration.testing.format.common.CommonFormatConfig;
import io.github.dimkich.integration.testing.format.xml.XmlConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import({CommonFormatConfig.class, XmlConfig.class, CompositeTestMapper.class})
public class TestFormatConfig {
}
