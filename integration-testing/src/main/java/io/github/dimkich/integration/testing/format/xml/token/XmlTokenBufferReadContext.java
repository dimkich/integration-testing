package io.github.dimkich.integration.testing.format.xml.token;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.databind.util.TokenBufferReadContext;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Set;

@Setter
@NoArgsConstructor
public class XmlTokenBufferReadContext extends TokenBufferReadContext {
    private Set<String> namesToWrap;

    protected XmlTokenBufferReadContext(JsonStreamContext base, ContentReference srcRef) {
        super(base, srcRef);
    }

    protected XmlTokenBufferReadContext(JsonStreamContext base, JsonLocation startLoc) {
        super(base, startLoc);
    }

    protected XmlTokenBufferReadContext(TokenBufferReadContext parent, int type, int index) {
        super(parent, type, index);
    }

    public static XmlTokenBufferReadContext createRootContext(JsonToken firstToken, JsonStreamContext origContext) {
        if (origContext == null) {
            return new XmlTokenBufferReadContext();
        }
        if (origContext.getParent() != null
                && (firstToken == JsonToken.START_OBJECT || firstToken == JsonToken.START_ARRAY)) {
            origContext = origContext.getParent();
        }
        return new XmlTokenBufferReadContext(origContext, ContentReference.unknown());
    }

    public boolean shouldWrap(String localName) {
        return (namesToWrap != null) && namesToWrap.contains(localName);
    }

    public void convertToArray() {
        _type = TYPE_ARRAY;
    }

    @Override
    public XmlTokenBufferReadContext createChildArrayContext() {
        ++_index;
        return new XmlTokenBufferReadContext(this, TYPE_ARRAY, -1);
    }

    @Override
    public XmlTokenBufferReadContext createChildObjectContext() {
        ++_index;
        return new XmlTokenBufferReadContext(this, TYPE_OBJECT, -1);
    }

    @Override
    public XmlTokenBufferReadContext parentOrCopy() {
        if (_parent instanceof XmlTokenBufferReadContext) {
            return (XmlTokenBufferReadContext) _parent;
        }
        if (_parent == null) {
            return new XmlTokenBufferReadContext();
        }
        return new XmlTokenBufferReadContext(_parent, _startLocation);
    }
}
