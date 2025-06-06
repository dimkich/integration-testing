package io.github.dimkich.integration.testing;

import java.time.ZonedDateTime;

public interface NowSetter {
    void setNow(ZonedDateTime dateTime);
}
