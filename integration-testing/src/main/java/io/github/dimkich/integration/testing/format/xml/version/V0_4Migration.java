package io.github.dimkich.integration.testing.format.xml.version;

import io.github.dimkich.integration.testing.format.xml.tnode.TNode;
import io.github.dimkich.integration.testing.util.TestUtils;

import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class V0_4Migration implements XmlMigration {
    private static final String TYPE_ATTRIBUTE = "type";
    private static final String UTYPE_ATTRIBUTE = "utype";
    private static final String CONNECTION_ATTRIBUTE = "connection";
    private static final String TOPIC_ATTRIBUTE = "topic";
    private static final String KEY_ATTRIBUTE = "key";
    private static final String KEY_ELEMENT = "key";
    private static final String PAYLOAD_ELEMENT = "payload";
    private static final String VALUE_ELEMENT = "value";
    private static final String HEADERS_ELEMENT = "headers";
    private static final String INBOUND_MESSAGE = "inboundMessage";
    private static final String OUTBOUND_MESSAGE = "outboundMessage";
    private static final String STRING_TYPE = "string";
    private static final String KAFKA_RECORD = "KafkaRecord";
    private static final String KAFKA_CONNECTION = "kafka";
    private static final Set<String> STRUCTURAL_ATTRIBUTES =
            Set.of(TOPIC_ATTRIBUTE, KEY_ATTRIBUTE, UTYPE_ATTRIBUTE, TYPE_ATTRIBUTE, CONNECTION_ATTRIBUTE);

    @Override
    public void migrate(String path) {
        migrate(TestUtils.getTestResourceFile(path));
    }

    /**
     * Migrates the given XML test file. The file is rewritten in place only when
     * the migration actually changed something.
     *
     * @param file the XML test file to migrate
     */
    public void migrate(File file) {
        TNode node = TNode.create(file);
        if (migrate(node)) {
            node.save(file);
        }
    }

    /**
     * Applies all 0.4 migrations to the given document root.
     *
     * @param root the root node of the test XML document
     * @return {@code true} if the document was changed and has to be saved
     */
    public boolean migrate(TNode root) {
        boolean changed = migrateMessages(root, INBOUND_MESSAGE);
        if (migrateMessages(root, OUTBOUND_MESSAGE)) {
            changed = true;
        }
        if (normalizeTypes(root)) {
            changed = true;
        }
        return changed;
    }

    private boolean migrateMessages(TNode root, String messageTag) {
        boolean changed = false;
        for (TNode message : root.findNodes(messageTag).toList()) {
            if (migrateMessage(message)) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean migrateMessage(TNode message) {
        if (isPayloadStyle(message)) {
            return migratePayloadMessage(message);
        }
        String utype = message.getAttributeValue(UTYPE_ATTRIBUTE);
        if (utype == null || message.getAttributeValue(TOPIC_ATTRIBUTE) == null) {
            return false;
        }
        String key = message.getAttributeValue(KEY_ATTRIBUTE);
        Map<String, String> headers = message.getAttributes();
        headers.keySet().removeIf(STRUCTURAL_ATTRIBUTES::contains);
        headers.keySet().forEach(message::removeAttribute);
        message.removeAttribute(KEY_ATTRIBUTE);
        message.removeAttribute(UTYPE_ATTRIBUTE);
        message.setAttributeValue(TYPE_ATTRIBUTE, KAFKA_RECORD);
        message.setAttributeValue(CONNECTION_ATTRIBUTE, KAFKA_CONNECTION);
        List<TNode> payload = removePayload(message);
        String payloadText = payload.isEmpty() ? message.getValue() : null;
        message.setValue(null);
        if (key != null) {
            message.addChild(KEY_ELEMENT, key);
        }
        addValue(message, utype, payload, payloadText);
        TNode headersNode = message.addChild(HEADERS_ELEMENT);
        headers.forEach(headersNode::addChild);
        return true;
    }

    private boolean migratePayloadMessage(TNode message) {
        TNode headers = findChild(message, HEADERS_ELEMENT);
        TNode payload = findChild(message, PAYLOAD_ELEMENT);
        TNode topicNode = headers == null ? null : findChild(headers, TOPIC_ATTRIBUTE);
        if (topicNode == null || topicNode.getValue() == null || payload == null) {
            return false;
        }
        TNode keyNode = findChild(headers, KEY_ELEMENT);
        Map<String, TNode> customHeaders = new LinkedHashMap<>();
        for (TNode header : headers.getChildNodes().toList()) {
            String name = header.getName();
            if (!TOPIC_ATTRIBUTE.equals(name) && !KEY_ELEMENT.equals(name)) {
                customHeaders.put(name, header);
            }
        }
        String keyAttribute = message.getAttributeValue(KEY_ATTRIBUTE);
        Map<String, String> attributes = new LinkedHashMap<>(message.getAttributes());
        attributes.keySet().removeIf(STRUCTURAL_ATTRIBUTES::contains);
        attributes.keySet().forEach(message::removeAttribute);

        String utype = payload.getAttributeValue(TYPE_ATTRIBUTE);
        List<TNode> payloadChildren = detachChildren(payload);
        String payloadText = payloadChildren.isEmpty() ? payload.getValue() : null;
        detachChildren(message);

        message.setAttributeValue(TOPIC_ATTRIBUTE, topicNode.getValue());
        message.setAttributeValue(TYPE_ATTRIBUTE, KAFKA_RECORD);
        message.setAttributeValue(CONNECTION_ATTRIBUTE, KAFKA_CONNECTION);
        if (keyNode != null) {
            copyContent(keyNode, message.addChild(KEY_ELEMENT));
        } else if (keyAttribute != null) {
            message.addChild(KEY_ELEMENT, keyAttribute);
        }
        addValue(message, utype, payloadChildren, payloadText);
        TNode headersNode = message.addChild(HEADERS_ELEMENT);
        customHeaders.forEach((name, node) -> copyContent(node, headersNode.addChild(name)));
        attributes.forEach(headersNode::addChild);
        return true;
    }

    private boolean normalizeTypes(TNode root) {
        boolean changed = false;
        for (TNode node : root.findNodes().toList()) {
            if (STRING_TYPE.equals(node.getAttributeValue(TYPE_ATTRIBUTE))) {
                node.removeAttribute(TYPE_ATTRIBUTE);
                changed = true;
            }
            if (capitalize(node, TYPE_ATTRIBUTE)) {
                changed = true;
            }
            if (capitalize(node, UTYPE_ATTRIBUTE)) {
                changed = true;
            }
        }
        return changed;
    }

    private boolean capitalize(TNode node, String attribute) {
        String value = node.getAttributeValue(attribute);
        if (value == null || value.isEmpty()) {
            return false;
        }
        String capitalized = value.substring(0, 1).toUpperCase() + value.substring(1);
        if (capitalized.equals(value)) {
            return false;
        }
        node.setAttributeValue(attribute, capitalized);
        return true;
    }

    /**
     * Legacy messages keep their content in {@code <headers>} and {@code <payload>}
     * child elements instead of attributes.
     */
    private boolean isPayloadStyle(TNode message) {
        return findChild(message, PAYLOAD_ELEMENT) != null;
    }

    private TNode findChild(TNode parent, String name) {
        return parent.findChildNodes(name).findFirst().orElse(null);
    }

    private List<TNode> detachChildren(TNode node) {
        List<TNode> children = node.getChildNodes().toList();
        children.forEach(TNode::remove);
        return children;
    }

    /**
     * Detaches all child nodes of the message. For the attribute style the whole
     * message body is the payload.
     */
    private List<TNode> removePayload(TNode message) {
        return detachChildren(message);
    }

    private void addValue(TNode message, String utype, List<TNode> payload, String payloadText) {
        TNode value = message.addChild(VALUE_ELEMENT);
        if (utype != null) {
            value.setAttributeValue(TYPE_ATTRIBUTE, utype);
        }
        value.setChildNodes(payload);
        if (payloadText != null) {
            value.setValue(payloadText);
        }
    }

    /**
     * Copies the type attribute and the content (text or child nodes) of the
     * source node into the target one.
     */
    private void copyContent(TNode source, TNode target) {
        String utype = source.getAttributeValue(TYPE_ATTRIBUTE);
        if (utype != null) {
            target.setAttributeValue(TYPE_ATTRIBUTE, utype);
        }
        List<TNode> children = detachChildren(source);
        if (children.isEmpty()) {
            String value = source.getValue();
            if (value != null) {
                target.setValue(value);
            }
        } else {
            target.setChildNodes(children);
        }
    }
}
