package com.regnosys.rosetta.common.serialisation.xml.deserialization;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

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

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Set;

/**
 * A token buffer which returns a parser that supports unwrapping of lists.
 * See issue <a href="https://github.com/FasterXML/jackson-dataformat-xml/issues/762">jackson-dataformat-xml#762</a>
 */
public class UnwrappableTokenBuffer extends TokenBuffer {
    public UnwrappableTokenBuffer(JsonParser p, DeserializationContext ctxt) {
        super(p, ctxt);
    }

    @Override
    public JsonParser asParser(ObjectCodec codec)
    {
        return new ElementWrappableParser(_first, codec, _hasNativeTypeIds, _hasNativeObjectIds, _parentContext, _streamReadConstraints);
    }

    @Override
    public JsonParser asParser(StreamReadConstraints streamReadConstraints)
    {
        return new ElementWrappableParser(_first, _objectCodec, _hasNativeTypeIds, _hasNativeObjectIds, _parentContext, streamReadConstraints);
    }

    @Override
    public JsonParser asParser(JsonParser src)
    {
        ElementWrappableParser p = new ElementWrappableParser(_first, src.getCodec(), _hasNativeTypeIds, _hasNativeObjectIds,
                _parentContext, src.streamReadConstraints());
        p.setLocation(src.currentTokenLocation());
        return p;
    }

    protected final static class ElementWrappableParser
            extends ParserMinimalBase implements ElementWrappable
    {
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
        protected Segment _segment;

        /**
         * Pointer to current token within current segment
         */
        protected int _segmentPtr;

        /**
         * Information about parser context, context in which
         * the next token is to be parsed (root, array, object).
         */
        protected UnwrappableTokenBufferReadContext _parsingContext;

        protected boolean _closed;

        protected transient ByteArrayBuilder _byteBuilder;

        protected JsonLocation _location = null;
        
        // CUSTOM
        protected String wrapperName = null;
        protected int wrapperState = 0;

        /*
        /**********************************************************
        /* Construction, init
        /**********************************************************
         */

        public ElementWrappableParser(Segment firstSeg, ObjectCodec codec,
                      boolean hasNativeTypeIds, boolean hasNativeObjectIds,
                      JsonStreamContext parentContext, StreamReadConstraints streamReadConstraints)
        {
            // 25-Jun-2022, tatu: Ideally would pass parser flags along (as
            //    per [databund#3528]) but for now make sure not to clear the flags
            //    but let defaults be used
            super();
            _segment = firstSeg;
            _segmentPtr = -1; // not yet read
            _codec = codec;
            _streamReadConstraints = streamReadConstraints;
            _parsingContext = UnwrappableTokenBufferReadContext.createRootContext(parentContext);
            _hasNativeTypeIds = hasNativeTypeIds;
            _hasNativeObjectIds = hasNativeObjectIds;
            _hasNativeIds = (hasNativeTypeIds || hasNativeObjectIds);
        }

        @Override
        public void addVirtualWrapping(Set<String> namesToWrap0, boolean caseInsensitive) {
            final Set<String> namesToWrap = caseInsensitive
                    ? CaseInsensitiveNameSet.construct(namesToWrap0)
                    : namesToWrap0;
            _parsingContext.setNamesToWrap(namesToWrap);
        }

        public void setLocation(JsonLocation l) {
            _location = l;
        }

        @Override
        public ObjectCodec getCodec() { return _codec; }

        @Override
        public void setCodec(ObjectCodec c) { _codec = c; }

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
            return DEFAULT_READ_CAPABILITIES;
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

        public JsonToken peekNextToken() throws IOException
        {
            // closed? nothing more to peek, either
            if (_closed) return null;
            Segment seg = _segment;
            int ptr = _segmentPtr+1;
            if (ptr >= Segment.TOKENS_PER_SEGMENT) {
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

        @Override
        public JsonToken nextToken() throws IOException
        {
            // If we are closed, nothing more to do
            if (_closed || (_segment == null)) return null;
            
            if (wrapperState > 0) {
                if (wrapperState == 1) {
                    wrapperState = 2;
                    _currToken = JsonToken.START_ARRAY;
                    return _currToken;
                } else if (wrapperState == 2) {
                    wrapperState = 3;
                    _currToken = JsonToken.FIELD_NAME;
                    return _currToken;
                } else if (wrapperState == 4) {
                    wrapperState = 0;
                    wrapperName = null;
                    _currToken = _segment.type(_segmentPtr);
                    return _currToken;
                }
            }

            // Ok, then: any more tokens?
            if (++_segmentPtr >= Segment.TOKENS_PER_SEGMENT) {
                _segmentPtr = 0;
                _segment = _segment.next();
                if (_segment == null) {
                    return null;
                }
            }
            JsonToken t = _segment.type(_segmentPtr);
            // Field name? Need to update context
            if (t == JsonToken.FIELD_NAME) {
                Object ob = _currentObject();
                String name = (ob instanceof String) ? ((String) ob) : ob.toString();
                if (wrapperState == 0) {
                    if (_parsingContext.shouldWrap(name)) {
                        wrapperName = name;
                        wrapperState = 1;
                    }
                } else if (wrapperState == 3) {
                    if (!_parsingContext.shouldWrap(name)) {
                        wrapperState = 4;
                        _currToken = JsonToken.END_ARRAY;
                        return _currToken;
                    }
                }
                _parsingContext.setCurrentName(name);
            } else if (t == JsonToken.START_OBJECT) {
                _parsingContext = _parsingContext.createChildObjectContext();
            } else if (t == JsonToken.START_ARRAY) {
                _parsingContext = _parsingContext.createChildArrayContext();
            } else if (t == JsonToken.END_OBJECT
                    || t == JsonToken.END_ARRAY) {
                if (wrapperState == 3) {
                    wrapperState = 4;
                    _currToken = JsonToken.END_ARRAY;
                    return _currToken;
                }
                // Closing JSON Object/Array? Close matching context
                _parsingContext = _parsingContext.parentOrCopy();
            } else {
                _parsingContext.updateForValue();
            }
            _currToken = t;
            return _currToken;
        }

        @Override
        public String nextTextValue() throws IOException {
            JsonToken t = nextToken();
            if (t == JsonToken.VALUE_STRING) {
                return getText();
            }
            if (t == JsonToken.FIELD_NAME) {
                JsonToken next = peekNextToken();
                if (next == JsonToken.VALUE_STRING) {
                    return nextTextValue();
                }
            }
            return null;
        }

        @Override
        public String nextFieldName() throws IOException
        {
            // inlined common case from nextToken()
            if (_closed || (_segment == null)) {
                return null;
            }

            int ptr = _segmentPtr+1;
            if ((ptr < Segment.TOKENS_PER_SEGMENT) && (_segment.type(ptr) == JsonToken.FIELD_NAME)) {
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
        public boolean isClosed() { return _closed; }

        /*
        /**********************************************************
        /* Public API, token accessors
        /**********************************************************
         */

        @Override
        public JsonStreamContext getParsingContext() { return _parsingContext; }

        @Override
        public JsonLocation currentLocation() {
            return (_location == null) ? JsonLocation.NA : _location;
        }

        @Override
        public JsonLocation currentTokenLocation() { return currentLocation(); }

        @Deprecated // since 2.17
        @Override
        public JsonLocation getTokenLocation() { return currentTokenLocation(); }

        @Deprecated // since 2.17
        @Override
        public JsonLocation getCurrentLocation() { return currentLocation(); }

        @Override
        public String currentName() {
            // 25-Jun-2015, tatu: as per [databind#838], needs to be same as ParserBase
            if (_currToken == JsonToken.START_OBJECT || _currToken == JsonToken.START_ARRAY) {
                JsonStreamContext parent = _parsingContext.getParent();
                return parent.getCurrentName();
            }
            return _parsingContext.getCurrentName();
        }

        @Override
        public void overrideCurrentName(String name)
        {
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

        @Deprecated // since 2.17
        @Override
        public String getCurrentName() { return currentName(); }

        /*
        /**********************************************************
        /* Public API, access to token information, text
        /**********************************************************
         */

        @Override
        public String getText()
        {
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
        public int getTextOffset() { return 0; }

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
                    double v = (Double) value;
                    return !Double.isFinite(v);
                }
                if (value instanceof Float) {
                    float v = (Float) value;
                    return !Double.isFinite(v);
                }
            }
            return false;
        }

        @Override
        public BigInteger getBigIntegerValue() throws IOException
        {
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
        public BigDecimal getDecimalValue() throws IOException
        {
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
        public int getIntValue() throws IOException
        {
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
        public NumberType getNumberType() throws IOException
        {
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
        public NumberTypeFP getNumberTypeFP() throws IOException
        {
            if (_currToken == JsonToken.VALUE_NUMBER_FLOAT) {
                Object n = _currentObject();
                if (n instanceof Double) return NumberTypeFP.DOUBLE64;
                if (n instanceof BigDecimal) return NumberTypeFP.BIG_DECIMAL;
                if (n instanceof Float) return NumberTypeFP.FLOAT32;
            }
            return NumberTypeFP.UNKNOWN;
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
                    // 08-Dec-2023, tatu: Note -- deferred numbers' validity (wrt input token)
                    //    has been verified by underlying `JsonParser`: no need to check again
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
                        throw new IllegalStateException("Internal error: failed to parse number '"+str+"'");
                    }
                    return dec;
                }
                return NumberInput.parseDouble(str, isEnabled(StreamReadFeature.USE_FAST_DOUBLE_PARSER));
            }
            throw new IllegalStateException("Internal error: entry should be a Number, but is of type "
                    +ClassUtil.classNameOf(value));
        }

        private final boolean _smallerThanInt(Number n) {
            return (n instanceof Short) || (n instanceof Byte);
        }

        private final boolean _smallerThanLong(Number n) {
            return (n instanceof Integer) || (n instanceof Short) || (n instanceof Byte);
        }

        // 02-Jan-2017, tatu: Modified from method(s) in `ParserBase`

        protected int _convertNumberToInt(Number n) throws IOException
        {
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

        protected long _convertNumberToLong(Number n) throws IOException
        {
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
        public Object getEmbeddedObject()
        {
            if (_currToken == JsonToken.VALUE_EMBEDDED_OBJECT) {
                return _currentObject();
            }
            return null;
        }

        @Override
        @SuppressWarnings("resource")
        public byte[] getBinaryValue(Base64Variant b64variant) throws IOException
        {
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
                throw _constructError("Current token ("+_currToken+") not VALUE_STRING (or VALUE_EMBEDDED_OBJECT with byte[]), cannot access as binary");
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
        public int readBinaryValue(Base64Variant b64variant, OutputStream out) throws IOException
        {
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
        public Object getTypeId() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getObjectId() {
            throw new UnsupportedOperationException();
        }

        /*
        /**********************************************************
        /* Internal methods
        /**********************************************************
         */

        protected final Object _currentObject() {
            return _segment.get(_segmentPtr);
        }

        protected final void _checkIsNumber() throws JacksonException
        {
            if (_currToken == null || !_currToken.isNumeric()) {
                throw _constructError("Current token ("+_currToken+") not numeric, cannot use numeric value accessors");
            }
        }

        @Override
        protected void _handleEOF() {
            _throwInternal();
        }
    }

}
