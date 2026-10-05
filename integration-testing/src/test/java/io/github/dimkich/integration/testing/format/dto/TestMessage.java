package io.github.dimkich.integration.testing.format.dto;

import io.github.dimkich.integration.testing.TestCase;
import io.github.dimkich.integration.testing.message.AbstractMessage;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class TestMessage extends AbstractMessage {

    public static TestCase testCase1() {
        TestCase tc = new TestCase();
        tc.setName("BugRepro");

        TestMessage inbound = new TestMessage();
        inbound.setConnection("conn1");
        inbound.setKey("k1");
        inbound.setHeaders(new LinkedHashMap<>());

        TestMessage outbound = new TestMessage();
        outbound.setConnection("conn1");
        outbound.setKey("k2");
        outbound.setHeaders(new LinkedHashMap<>());

        tc.setInboundMessage(inbound);
        tc.setOutboundMessages(List.of(outbound));
        return tc;
    }

    public static TestCase testCase2() {
        TestCase tc = new TestCase();
        tc.setName("EmptyByteKey");

        TestMessage inbound = new TestMessage();
        inbound.setConnection("conn1");
        inbound.setKey(new byte[0]);
        inbound.setHeaders(new LinkedHashMap<>());

        TestMessage outbound = new TestMessage();
        outbound.setConnection("conn1");
        outbound.setKey(new byte[]{1, 2, 3});
        outbound.setHeaders(new LinkedHashMap<>());

        tc.setInboundMessage(inbound);
        tc.setOutboundMessages(List.of(outbound));
        return tc;
    }

    private Object key;
    private Map<String, Object> headers = new TreeMap<>();

    @Override
    public Object identity() {
        return null;
    }
}
