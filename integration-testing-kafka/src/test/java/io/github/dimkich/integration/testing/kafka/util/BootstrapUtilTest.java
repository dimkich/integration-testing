package io.github.dimkich.integration.testing.kafka.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;

class BootstrapUtilTest {

    private static InetAddress localhost;
    private static InetAddress ip1;
    private static InetAddress ip2;

    @BeforeAll
    static void setUp() throws UnknownHostException {
        localhost = InetAddress.getByAddress(new byte[]{127, 0, 0, 1});
        ip1 = InetAddress.getByAddress(new byte[]{1, 1, 1, 1});
        ip2 = InetAddress.getByAddress(new byte[]{2, 2, 2, 2});
    }

    @AfterEach
    void clearCache() throws Exception {
        java.lang.reflect.Field field = BootstrapUtil.class.getDeclaredField("CACHE");
        field.setAccessible(true);
        ((ConcurrentHashMap<?, ?>) field.get(null)).clear();
    }

    @Test
    void normalizeNull() {
        assertThat(BootstrapUtil.normalize(null)).isNull();
    }

    @Test
    void normalizeEmpty() {
        assertThat(BootstrapUtil.normalize("")).isEmpty();
    }

    @Test
    void normalizeSingleHost() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(() -> InetAddress.getAllByName("localhost"))
                    .thenReturn(new InetAddress[]{localhost});

            String result = BootstrapUtil.normalize("localhost:9092");

            assertThat(result).isEqualTo("127.0.0.1:9092");
        }
    }

    @Test
    void normalizeMultipleHostsSorted() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(() -> InetAddress.getAllByName("host2"))
                    .thenReturn(new InetAddress[]{ip2});
            mocked.when(() -> InetAddress.getAllByName("host1"))
                    .thenReturn(new InetAddress[]{ip1});

            String result = BootstrapUtil.normalize("host2:9092,host1:9092");

            assertThat(result).isEqualTo("1.1.1.1:9092,2.2.2.2:9092");
        }
    }

    @Test
    void normalizeTrimSpaces() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(() -> InetAddress.getAllByName("host1"))
                    .thenReturn(new InetAddress[]{ip1});
            mocked.when(() -> InetAddress.getAllByName("host2"))
                    .thenReturn(new InetAddress[]{ip2});

            String result = BootstrapUtil.normalize(" host1:9092 , host2:9092 ");

            assertThat(result).isEqualTo("1.1.1.1:9092,2.2.2.2:9092");
        }
    }

    @Test
    void normalizeUnknownHost() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(() -> InetAddress.getAllByName("unknown"))
                    .thenThrow(new UnknownHostException("unknown"));

            String result = BootstrapUtil.normalize("unknown:9092");

            assertThat(result).isEqualTo("unknown:9092");
        }
    }

    @Test
    void normalizeMultiIP() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(() -> InetAddress.getAllByName("host1"))
                    .thenReturn(new InetAddress[]{ip2, ip1});

            String result = BootstrapUtil.normalize("host1:9092");

            assertThat(result).isEqualTo("1.1.1.1|2.2.2.2:9092");
        }
    }

    @Test
    void normalizeTrailingComma() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(() -> InetAddress.getAllByName("host1"))
                    .thenReturn(new InetAddress[]{ip1});

            String result = BootstrapUtil.normalize("host1:9092,");

            assertThat(result).isEqualTo("1.1.1.1:9092");
        }
    }

    @Test
    void normalizeNoPort() {
        String result = BootstrapUtil.normalize("localhost");

        assertThat(result).isEqualTo("localhost");
    }

    @Test
    void normalizeMemoization() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(() -> InetAddress.getAllByName("host1"))
                    .thenReturn(new InetAddress[]{ip1});

            String first = BootstrapUtil.normalize("host1:9092");
            String second = BootstrapUtil.normalize("host1:9092");

            assertThat(first).isSameAs(second);
            mocked.verify(() -> InetAddress.getAllByName("host1"), times(1));
        }
    }

    @Test
    void normalizeIPv6Bracketed() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(() -> InetAddress.getAllByName("::1"))
                    .thenReturn(new InetAddress[]{ip1});

            String result = BootstrapUtil.normalize("[::1]:9092");

            assertThat(result).isEqualTo("1.1.1.1:9092");
        }
    }

    @Test
    void normalizeDeduplicatesIPs() {
        try (MockedStatic<InetAddress> mocked = mockStatic(InetAddress.class)) {
            mocked.when(() -> InetAddress.getAllByName("host1"))
                    .thenReturn(new InetAddress[]{ip1, ip1});

            String result = BootstrapUtil.normalize("host1:9092");

            assertThat(result).isEqualTo("1.1.1.1:9092");
        }
    }

    @Test
    void asStringKeepsString() {
        assertThat(BootstrapUtil.asString("host1:9092,host2:9092")).isEqualTo("host1:9092,host2:9092");
    }

    @Test
    void asStringJoinsList() {
        assertThat(BootstrapUtil.asString(List.of("host1:9092", "host2:9092")))
                .isEqualTo("host1:9092,host2:9092");
    }

    @Test
    void asStringSingleElementList() {
        assertThat(BootstrapUtil.asString(List.of("host1:9092"))).isEqualTo("host1:9092");
    }

    @Test
    void asStringNull() {
        assertThat(BootstrapUtil.asString(null)).isNull();
    }
}
