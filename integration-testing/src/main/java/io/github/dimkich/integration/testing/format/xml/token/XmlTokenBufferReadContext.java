package io.github.dimkich.integration.testing.format.xml.token;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.databind.util.TokenBufferReadContext;

import java.util.Set;

public class XmlTokenBufferReadContext extends TokenBufferReadContext {
    private Set<String> _namesToWrap;

    public XmlTokenBufferReadContext() {
    }

    protected XmlTokenBufferReadContext(JsonStreamContext base, JsonLocation startLoc) {
        super(base, startLoc);
    }

    protected XmlTokenBufferReadContext(TokenBufferReadContext parent, int type, int index) {
        super(parent, type, index);
    }

    public void setNamesToWrap(Set<String> namesToWrap) {
        _namesToWrap = namesToWrap;
    }

    public boolean shouldWrap(String localName) {
        return (_namesToWrap != null) && _namesToWrap.contains(localName);
    }

    @Override
    public TokenBufferReadContext parentOrCopy() {
        if (_parent instanceof TokenBufferReadContext) {
            return (TokenBufferReadContext) _parent;
        }
        if (_parent == null) {
            return new XmlTokenBufferReadContext();
        }
        return new XmlTokenBufferReadContext(_parent, _startLocation);
    }

    @Override
    public TokenBufferReadContext createChildArrayContext() {
        ++_index;
        return new XmlTokenBufferReadContext(this, JsonStreamContext.TYPE_ARRAY, -1);
    }

    @Override
    public TokenBufferReadContext createChildObjectContext() {
        ++_index;
        return new XmlTokenBufferReadContext(this, JsonStreamContext.TYPE_OBJECT, -1);
    }
}
