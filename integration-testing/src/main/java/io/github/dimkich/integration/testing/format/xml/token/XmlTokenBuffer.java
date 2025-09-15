package io.github.dimkich.integration.testing.format.xml.token;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.base.ParserMinimalBase;
import com.fasterxml.jackson.core.io.NumberInput;
import com.fasterxml.jackson.core.util.ByteArrayBuilder;
import com.fasterxml.jackson.core.util.JacksonFeatureSet;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.util.ClassUtil;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.fasterxml.jackson.databind.util.TokenBufferReadContext;
import com.fasterxml.jackson.dataformat.xml.deser.ElementWrappable;
import com.fasterxml.jackson.dataformat.xml.util.CaseInsensitiveNameSet;
import lombok.SneakyThrows;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Objects;
import java.util.Set;

public class XmlTokenBuffer extends TokenBuffer {
    private JsonToken currentToken;

    public XmlTokenBuffer(JsonParser p, DeserializationContext ctxt) {
        super(p, ctxt);
        currentToken = p.currentToken();
    }

    @Override
    public JsonParser asParser(ObjectCodec codec) {
        return new XmlTokenParser(_first, codec, _hasNativeTypeIds, _hasNativeObjectIds, _parentContext,
                _streamReadConstraints, currentToken);
    }

    @Override
    public JsonParser asParser(StreamReadConstraints streamReadConstraints) {
        return new XmlTokenParser(_first, _objectCodec, _hasNativeTypeIds, _hasNativeObjectIds, _parentContext,
                streamReadConstraints, currentToken);
    }

    @Override
    public JsonParser asParser(JsonParser src) {
        XmlTokenParser p = new XmlTokenParser(_first, src.getCodec(), _hasNativeTypeIds, _hasNativeObjectIds,
                _parentContext, src.streamReadConstraints(), currentToken);
        p.setLocation(src.currentTokenLocation());
        return p;
    }

    public static class XmlTokenParser extends ParserMinimalBase implements ElementWrappable {
        protected final static JacksonFeatureSet<StreamReadCapability> XML_READ_CAPABILITIES =
                DEFAULT_READ_CAPABILITIES
                        .with(StreamReadCapability.DUPLICATE_PROPERTIES)
                        .with(StreamReadCapability.SCALARS_AS_OBJECTS)
                        .with(StreamReadCapability.UNTYPED_SCALARS);

        /*
        /**********************************************************
        /* Configuration
        /**********************************************************
         */

        protected ObjectCodec _codec;

        /**
         * @since 2.15
         */
        protected StreamReadConstraints _streamReadConstraints;

        /**
         * @since 2.3
         */
        protected final boolean _hasNativeTypeIds;

        /**
         * @since 2.3
         */
        protected final boolean _hasNativeObjectIds;

        protected final boolean _hasNativeIds;

        /*
        /**********************************************************
        /* Parsing state
        /**********************************************************
         */

        /**
         * Currently active segment
         */
        protected TokenBuffer.Segment _segment;

        /**
         * Pointer to current token within current segment
         */
        protected int _segmentPtr;

        /**
         * Information about parser context, context in which
         * the next token is to be parsed (root, array, object).
         */
        protected XmlTokenBufferReadContext _parsingContext;

        protected boolean _closed;

        protected transient ByteArrayBuilder _byteBuilder;

        protected JsonLocation _location = null;
        private final Method findTypeId;
        private final Method findObjectId;

        /*
        /**********************************************************
        /* Construction, init
        /**********************************************************
         */
        @SneakyThrows
        public XmlTokenParser(TokenBuffer.Segment firstSeg, ObjectCodec codec, boolean hasNativeTypeIds,
                              boolean hasNativeObjectIds, JsonStreamContext parentContext,
                              StreamReadConstraints streamReadConstraints, JsonToken currentToken) {
            // 25-Jun-2022, tatu: Ideally would pass parser flags along (as
            //    per [databund#3528]) but for now make sure not to clear the flags
            //    but let defaults be used
            super();
            _segment = firstSeg;
            _segmentPtr = -1; // not yet read
            _codec = codec;
            _streamReadConstraints = streamReadConstraints;
            _parsingContext = XmlTokenBufferReadContext.createRootContext(currentToken, parentContext);
            _hasNativeTypeIds = hasNativeTypeIds;
            _hasNativeObjectIds = hasNativeObjectIds;
            _hasNativeIds = (hasNativeTypeIds || hasNativeObjectIds);

            findTypeId = TokenBuffer.Segment.class.getDeclaredMethod("findTypeId", int.class);
            findTypeId.setAccessible(true);
            findObjectId = TokenBuffer.Segment.class.getDeclaredMethod("findObjectId", int.class);
            findObjectId.setAccessible(true);
        }

        @Override
        public void addVirtualWrapping(Set<String> namesToWrap0, boolean caseInsensitive) {
            final Set<String> namesToWrap = caseInsensitive
                    ? CaseInsensitiveNameSet.construct(namesToWrap0)
                    : namesToWrap0;

            if (!_parsingContext.inRoot()
                    && !_parsingContext.getParent().inRoot()) {
                String name = currentName();
                if ((name != null) && namesToWrap.contains(name)) {
                    nextToken.addLast(JsonToken.START_OBJECT);
                }
            }
            _parsingContext.setNamesToWrap(namesToWrap);
        }

        @Override
        @SneakyThrows
        public boolean isExpectedStartArrayToken() {
            JsonToken t = getCurrentToken();
            if (t == JsonToken.START_OBJECT) {
                _parsingContext.convertToArray();
                _parsingContext.setCurrentName(getParsingContext().getParent().getCurrentName());
                _currToken = t = JsonToken.START_ARRAY;
            }
            return (t == JsonToken.START_ARRAY);
        }

        public void setLocation(JsonLocation l) {
            _location = l;
        }

        @Override
        public ObjectCodec getCodec() {
            return _codec;
        }

        @Override
        public void setCodec(ObjectCodec c) {
            _codec = c;
        }

        /*
        /**********************************************************
        /* Public API, config access, capability introspection
        /**********************************************************
         */

        @Override
        public Version version() {
            return com.fasterxml.jackson.databind.cfg.PackageVersion.VERSION;
        }

        // 20-May-2020, tatu: This may or may not be enough -- ideally access is
        //    via `DeserializationContext`, not parser, but if latter is needed
        //    then we'll need to pass this from parser contents if which were
        //    buffered.
        @Override
        public JacksonFeatureSet<StreamReadCapability> getReadCapabilities() {
            return XML_READ_CAPABILITIES;
        }

        @Override
        public StreamReadConstraints streamReadConstraints() {
            return _streamReadConstraints;
        }

        /*
        /**********************************************************
        /* Extended API beyond JsonParser
        /**********************************************************
         */

        public JsonToken peekNextToken() throws IOException {
            // closed? nothing more to peek, either
            if (_closed) return null;
            TokenBuffer.Segment seg = _segment;
            int ptr = _segmentPtr + 1;
            if (ptr >= TokenBuffer.Segment.TOKENS_PER_SEGMENT) {
                ptr = 0;
                seg = (seg == null) ? null : seg.next();
            }
            return (seg == null) ? null : seg.type(ptr);
        }

        /*
        /**********************************************************
        /* Closeable implementation
        /**********************************************************
         */

        @Override
        public void close() throws IOException {
            if (!_closed) {
                _closed = true;
            }
        }

        /*
        /**********************************************************
        /* Public API, traversal
        /**********************************************************
         */

        private final Deque<JsonToken> nextToken = new ArrayDeque<>();
        private boolean repeatToken = false;

        private JsonToken _nextToken() {
            // If we are closed, nothing more to do
            if (_closed || (_segment == null)) return null;

            if (repeatToken) {
                _currToken = _segment.type(_segmentPtr);
                repeatToken = false;
            } else if (!nextToken.isEmpty()) {
                _currToken = nextToken.pop();
            } else {
                // Ok, then: any more tokens?
                if (++_segmentPtr >= TokenBuffer.Segment.TOKENS_PER_SEGMENT) {
                    _segmentPtr = 0;
                    _segment = _segment.next();
                    if (_segment == null) {
                        return null;
                    }
                }
                _currToken = _segment.type(_segmentPtr);
            }
            return _currToken;
        }

        @Override
        public JsonToken nextToken() throws IOException {
            _nextToken();
            if (_currToken == JsonToken.FIELD_NAME && _parsingContext.inArray()) {
                Object ob = _currentObject();
                String name = (ob instanceof String) ? ((String) ob) : ob.toString();
                if (_parsingContext.getCurrentName() == null || Objects.equals(_parsingContext.getCurrentName(), name)) {
                    _nextToken();
                } else {
                    _currToken = JsonToken.END_OBJECT;
                    repeatToken = true;
                }
            }

            // Field name? Need to update context
            if (_currToken == JsonToken.FIELD_NAME) {
                Object ob = _currentObject();
                String name = (ob instanceof String) ? ((String) ob) : ob.toString();
                if (!name.equals(_parsingContext.getCurrentName()) && _parsingContext.shouldWrap(name)) {
                    nextToken.addLast(JsonToken.START_OBJECT);
                }
                _parsingContext.setCurrentName(name);
            } else if (_currToken == JsonToken.START_OBJECT) {
                _parsingContext = _parsingContext.createChildObjectContext();
            } else if (_currToken == JsonToken.START_ARRAY) {
                _parsingContext = _parsingContext.createChildArrayContext();
            } else if (_currToken == JsonToken.END_OBJECT || _currToken == JsonToken.END_ARRAY) {
                // Closing JSON Object/Array? Close matching context
                if (_parsingContext.inArray()) {
                    _currToken = JsonToken.END_ARRAY;
                }
                _parsingContext = _parsingContext.parentOrCopy();
                String name = _parsingContext.getCurrentName();
                if (_parsingContext.shouldWrap(name) && !repeatToken) {
                    nextToken.addLast(JsonToken.END_OBJECT);
                }
            } else {
                _parsingContext.updateForValue();
            }
            return _currToken;
        }

        @Override
        public String nextFieldName() throws IOException {
            // inlined common case from nextToken()
            if (_closed || (_segment == null)) {
                return null;
            }

            int ptr = _segmentPtr + 1;
            if ((ptr < TokenBuffer.Segment.TOKENS_PER_SEGMENT) && (_segment.type(ptr) == JsonToken.FIELD_NAME)) {
                _segmentPtr = ptr;
                _currToken = JsonToken.FIELD_NAME;
                Object ob = _segment.get(ptr); // inlined _currentObject();
                String name = (ob instanceof String) ? ((String) ob) : ob.toString();
                _parsingContext.setCurrentName(name);
                return name;
            }
            return (nextToken() == JsonToken.FIELD_NAME) ? currentName() : null;
        }

        @Override
        public boolean isClosed() {
            return _closed;
        }

        /*
        /**********************************************************
        /* Public API, token accessors
        /**********************************************************
         */

        @Override
        public JsonStreamContext getParsingContext() {
            return _parsingContext;
        }

        @Override
        public JsonLocation getTokenLocation() {
            return getCurrentLocation();
        }

        @Override
        public JsonLocation getCurrentLocation() {
            return (_location == null) ? JsonLocation.NA : _location;
        }

        @Override
        public String currentName() {
            // 25-Jun-2015, tatu: as per [databind#838], needs to be same as ParserBase
            if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
                JsonStreamContext parent = _parsingContext.getParent();
                return parent.getCurrentName();
            }
            return _parsingContext.getCurrentName();
        }

        @Override // since 2.12 delegate to the new method
        public String getCurrentName() {
            return currentName();
        }

        @Override
        public void overrideCurrentName(String name) {
            // Simple, but need to look for START_OBJECT/ARRAY's "off-by-one" thing:
            JsonStreamContext ctxt = _parsingContext;
            if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
                ctxt = ctxt.getParent();
            }
            if (ctxt instanceof TokenBufferReadContext) {
                try {
                    ((TokenBufferReadContext) ctxt).setCurrentName(name);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        /*
        /**********************************************************
        /* Public API, access to token information, text
        /**********************************************************
         */

        @Override
        public String getText() {
            // common cases first:
            if (_currToken == JsonToken.VALUE_STRING
                    || _currToken == JsonToken.FIELD_NAME) {
                Object ob = _currentObject();
                if (ob instanceof String) {
                    return (String) ob;
                }
                return ClassUtil.nullOrToString(ob);
            }
            if (_currToken == null) {
                return null;
            }
            switch (_currToken) {
                case VALUE_NUMBER_INT:
                case VALUE_NUMBER_FLOAT:
                    return ClassUtil.nullOrToString(_currentObject());
                default:
                    return _currToken.asString();
            }
        }

        @Override
        public char[] getTextCharacters() {
            String str = getText();
            return (str == null) ? null : str.toCharArray();
        }

        @Override
        public int getTextLength() {
            String str = getText();
            return (str == null) ? 0 : str.length();
        }

        @Override
        public int getTextOffset() {
            return 0;
        }

        @Override
        public boolean hasTextCharacters() {
            // We never have raw buffer available, so:
            return false;
        }

        /*
        /**********************************************************
        /* Public API, access to token information, numeric
        /**********************************************************
         */

        @Override
        public boolean isNaN() {
            // can only occur for floating-point numbers
            if (_currToken == JsonToken.VALUE_NUMBER_FLOAT) {
                Object value = _currentObject();
                if (value instanceof Double) {
                    Double v = (Double) value;
                    return v.isNaN() || v.isInfinite();
                }
                if (value instanceof Float) {
                    Float v = (Float) value;
                    return v.isNaN() || v.isInfinite();
                }
            }
            return false;
        }

        @Override
        public BigInteger getBigIntegerValue() throws IOException {
            Number n = getNumberValue(true);
            if (n instanceof BigInteger) {
                return (BigInteger) n;
            } else if (n instanceof BigDecimal) {
                final BigDecimal bd = (BigDecimal) n;
                streamReadConstraints().validateBigIntegerScale(bd.scale());
                return bd.toBigInteger();
            }
            // int/long is simple, but let's also just truncate float/double:
            return BigInteger.valueOf(n.longValue());
        }

        @Override
        public BigDecimal getDecimalValue() throws IOException {
            Number n = getNumberValue(true);
            if (n instanceof BigDecimal) {
                return (BigDecimal) n;
            } else if (n instanceof Integer) {
                return BigDecimal.valueOf(n.intValue());
            } else if (n instanceof Long) {
                return BigDecimal.valueOf(n.longValue());
            } else if (n instanceof BigInteger) {
                return new BigDecimal((BigInteger) n);
            }
            // float or double
            return BigDecimal.valueOf(n.doubleValue());
        }

        @Override
        public double getDoubleValue() throws IOException {
            return getNumberValue().doubleValue();
        }

        @Override
        public float getFloatValue() throws IOException {
            return getNumberValue().floatValue();
        }

        @Override
        public int getIntValue() throws IOException {
            Number n = (_currToken == JsonToken.VALUE_NUMBER_INT) ?
                    ((Number) _currentObject()) : getNumberValue();
            if ((n instanceof Integer) || _smallerThanInt(n)) {
                return n.intValue();
            }
            return _convertNumberToInt(n);
        }

        @Override
        public long getLongValue() throws IOException {
            Number n = (_currToken == JsonToken.VALUE_NUMBER_INT) ?
                    ((Number) _currentObject()) : getNumberValue();
            if ((n instanceof Long) || _smallerThanLong(n)) {
                return n.longValue();
            }
            return _convertNumberToLong(n);
        }

        @Override
        public NumberType getNumberType() throws IOException {
            Object n = getNumberValueDeferred();
            if (n instanceof Integer) return NumberType.INT;
            if (n instanceof Long) return NumberType.LONG;
            if (n instanceof Double) return NumberType.DOUBLE;
            if (n instanceof BigDecimal) return NumberType.BIG_DECIMAL;
            if (n instanceof BigInteger) return NumberType.BIG_INTEGER;
            if (n instanceof Float) return NumberType.FLOAT;
            if (n instanceof Short) return NumberType.INT;       // should be SHORT
            if (n instanceof String) {
                return (_currToken == JsonToken.VALUE_NUMBER_FLOAT)
                        ? NumberType.BIG_DECIMAL : NumberType.BIG_INTEGER;
            }
            return null;
        }

        @Override
        public final Number getNumberValue() throws IOException {
            return getNumberValue(false);
        }

        @Override
        public Object getNumberValueDeferred() throws IOException {
            _checkIsNumber();
            return _currentObject();
        }

        private Number getNumberValue(final boolean preferBigNumbers) throws IOException {
            _checkIsNumber();
            Object value = _currentObject();
            if (value instanceof Number) {
                return (Number) value;
            }
            // Difficult to really support numbers-as-Strings; but let's try.
            // NOTE: no access to DeserializationConfig, unfortunately, so cannot
            // try to determine Double/BigDecimal preference...
            if (value instanceof String) {
                String str = (String) value;
                final int len = str.length();
                if (_currToken == JsonToken.VALUE_NUMBER_INT) {
                    if (preferBigNumbers
                            // 01-Feb-2023, tatu: Not really accurate but we'll err on side
                            //   of not losing accuracy (should really check 19-char case,
                            //   or, with minus sign, 20-char)
                            || (len >= 19)) {
                        return NumberInput.parseBigInteger(str, isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
                    }
                    // Otherwise things get trickier; here, too, we should use more accurate
                    // boundary checks
                    if (len >= 10) {
                        return NumberInput.parseLong(str);
                    }
                    return NumberInput.parseInt(str);
                }
                if (preferBigNumbers) {
                    BigDecimal dec = NumberInput.parseBigDecimal(str,
                            isEnabled(StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER));
                    // 01-Feb-2023, tatu: This is... weird. Seen during tests, only
                    if (dec == null) {
                        throw new IllegalStateException("Internal error: failed to parse number '" + str + "'");
                    }
                    return dec;
                }
                return NumberInput.parseDouble(str, isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
            }
            throw new IllegalStateException("Internal error: entry should be a Number, but is of type "
                    + ClassUtil.classNameOf(value));
        }

        private final boolean _smallerThanInt(Number n) {
            return (n instanceof Short) || (n instanceof Byte);
        }

        private final boolean _smallerThanLong(Number n) {
            return (n instanceof Integer) || (n instanceof Short) || (n instanceof Byte);
        }

        // 02-Jan-2017, tatu: Modified from method(s) in `ParserBase`

        protected int _convertNumberToInt(Number n) throws IOException {
            if (n instanceof Long) {
                long l = n.longValue();
                int result = (int) l;
                if (((long) result) != l) {
                    reportOverflowInt();
                }
                return result;
            }
            if (n instanceof BigInteger) {
                BigInteger big = (BigInteger) n;
                if (BI_MIN_INT.compareTo(big) > 0
                        || BI_MAX_INT.compareTo(big) < 0) {
                    reportOverflowInt();
                }
            } else if ((n instanceof Double) || (n instanceof Float)) {
                double d = n.doubleValue();
                // Need to check boundaries
                if (d < MIN_INT_D || d > MAX_INT_D) {
                    reportOverflowInt();
                }
                return (int) d;
            } else if (n instanceof BigDecimal) {
                BigDecimal big = (BigDecimal) n;
                if (BD_MIN_INT.compareTo(big) > 0
                        || BD_MAX_INT.compareTo(big) < 0) {
                    reportOverflowInt();
                }
            } else {
                _throwInternal();
            }
            return n.intValue();
        }

        protected long _convertNumberToLong(Number n) throws IOException {
            if (n instanceof BigInteger) {
                BigInteger big = (BigInteger) n;
                if (BI_MIN_LONG.compareTo(big) > 0
                        || BI_MAX_LONG.compareTo(big) < 0) {
                    reportOverflowLong();
                }
            } else if ((n instanceof Double) || (n instanceof Float)) {
                double d = n.doubleValue();
                // Need to check boundaries
                if (d < MIN_LONG_D || d > MAX_LONG_D) {
                    reportOverflowLong();
                }
                return (long) d;
            } else if (n instanceof BigDecimal) {
                BigDecimal big = (BigDecimal) n;
                if (BD_MIN_LONG.compareTo(big) > 0
                        || BD_MAX_LONG.compareTo(big) < 0) {
                    reportOverflowLong();
                }
            } else {
                _throwInternal();
            }
            return n.longValue();
        }

        /*
        /**********************************************************
        /* Public API, access to token information, other
        /**********************************************************
         */

        @Override
        public Object getEmbeddedObject() {
            if (_currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
                return _currentObject();
            }
            return null;
        }

        @Override
        @SuppressWarnings("resource")
        public byte[] getBinaryValue(Base64Variant b64variant) throws IOException {
            // First: maybe we some special types?
            if (_currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
                // Embedded byte array would work nicely...
                Object ob = _currentObject();
                if (ob instanceof byte[]) {
                    return (byte[]) ob;
                }
                // fall through to error case
            }
            if (_currToken != JsonToken.VALUE_STRING) {
                throw _constructError("Current token (" + _currToken + ") not VALUE_STRING (or VALUE_EMBEDDED_OBJECT with byte[]), cannot access as binary");
            }
            final String str = getText();
            if (str == null) {
                return null;
            }
            ByteArrayBuilder builder = _byteBuilder;
            if (builder == null) {
                _byteBuilder = builder = new ByteArrayBuilder(100);
            } else {
                _byteBuilder.reset();
            }
            _decodeBase64(str, builder, b64variant);
            return builder.toByteArray();
        }

        @Override
        public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException {
            byte[] data = getBinaryValue(b64variant);
            if (data != null) {
                out.write(data, 0, data.length);
                return data.length;
            }
            return 0;
        }

        /*
        /**********************************************************
        /* Public API, native ids
        /**********************************************************
         */

        @Override
        public boolean canReadObjectId() {
            return _hasNativeObjectIds;
        }

        @Override
        public boolean canReadTypeId() {
            return _hasNativeTypeIds;
        }

        @Override
        @SneakyThrows
        public Object getTypeId() {
            return findTypeId.invoke(_segment, _segmentPtr);
        }

        @Override
        @SneakyThrows
        public Object getObjectId() {
            return findObjectId.invoke(_segment, _segmentPtr);
        }

        /*
        /**********************************************************
        /* Internal methods
        /**********************************************************
         */

        protected final Object _currentObject() {
            return _segment.get(_segmentPtr);
        }

        protected final void _checkIsNumber() throws JacksonException {
            if (_currToken == null || !_currToken.isNumeric()) {
                throw _constructError("Current token (" + _currToken + ") not numeric, cannot use numeric value accessors");
            }
        }

        @Override
        protected void _handleEOF() {
            _throwInternal();
        }
    }
}
