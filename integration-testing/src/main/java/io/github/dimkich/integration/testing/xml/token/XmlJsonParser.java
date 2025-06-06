package io.github.dimkich.integration.testing.xml.token;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.util.JsonParserDelegate;
import lombok.SneakyThrows;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Set;

public class XmlJsonParser extends JsonParserDelegate {
    private JsonToken currentToken;
    private final Deque<JsonToken> nextToken = new ArrayDeque<>();

    public XmlJsonParser(JsonParser d) {
        super(d);
        setContext(new XmlTokenBufferReadContext());
    }

    @Override
    @SneakyThrows
    public boolean isExpectedStartArrayToken() {
        JsonToken t = getCurrentToken();
        if (t == JsonToken.START_OBJECT) {
            String name = getCurrentName();
            setContext(getContext().parentOrCopy());
            setContext(getContext().createChildArrayContext());
            getContext().setCurrentName(name);
            currentToken = JsonToken.START_ARRAY;
            return true;
        }
        return (t == JsonToken.START_ARRAY);
    }


    @Override
    @SneakyThrows
    public JsonToken nextToken() {
        if (!nextToken.isEmpty()) {
            currentToken = nextToken.pop();
            return currentToken;
        }
        boolean inArray = getParsingContext().inArray();
        String oldName = currentName();
        currentToken = delegate.nextToken();
        String newName = currentName();
        if (inArray && (currentToken == JsonToken.END_OBJECT
                || (currentToken == JsonToken.FIELD_NAME && !newName.equals(oldName)) && oldName != null)) {
            setContext(getContext().parentOrCopy());
            getContext().setCurrentName(newName);
            nextToken.addLast(currentToken);
            currentToken = JsonToken.END_ARRAY;
        } else if (inArray && currentToken == JsonToken.FIELD_NAME) {
            currentToken = delegate.nextToken();
        }
        if (currentToken == JsonToken.FIELD_NAME && getContext().shouldWrap(currentName())) {
            nextToken.addLast(JsonToken.START_OBJECT);
        }
        return currentToken;
    }

    @Override
    public String nextTextValue() throws IOException {
        JsonToken jsonToken = nextToken();
        if (jsonToken == JsonToken.FIELD_NAME) {
            jsonToken = nextToken();
        }
        if (jsonToken == JsonToken.VALUE_STRING) {
            return getText();
        }
        return null;
    }

    private XmlTokenBufferReadContext getContext() {
        return (XmlTokenBufferReadContext) getParsingContext();
    }

    @SneakyThrows
    private void setContext(JsonStreamContext context) {
        XmlTokenBuffer.parserParsingContextField.set(delegate, context);
    }

    public void addVirtualWrapping(Set<String> namesToWrap, boolean caseInsensitive) {
        getContext().setNamesToWrap(namesToWrap);
    }

    @Override
    public JsonToken currentToken() {
        return currentToken;
    }

    @Override
    public JsonToken getCurrentToken() {
        return currentToken;
    }
}
