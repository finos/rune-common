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

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.io.ContentReference;
import com.fasterxml.jackson.databind.util.TokenBufferReadContext;
import com.fasterxml.jackson.dataformat.xml.deser.XmlReadContext;

import java.util.Set;

/**
 * An extension of {@link TokenBufferReadContext} which add support for remembering names to wrap.
 * 
 * The implementation is the same as in {@link XmlReadContext}. It adds two methods
 * `setNamesToWrap` and `setNamesToWrap`.
 */
public class UnwrappableTokenBufferReadContext extends TokenBufferReadContext {

    protected Set<String> _namesToWrap;
    protected String _wrapperName = null;
    protected int _wrapperState = 0;

    protected UnwrappableTokenBufferReadContext(JsonStreamContext base, ContentReference srcRef)
    {
        super(base, srcRef);
    }

    protected UnwrappableTokenBufferReadContext(JsonStreamContext base, JsonLocation startLoc) {
        super(base, startLoc);
    }

    protected UnwrappableTokenBufferReadContext() {
        super();
    }

    protected UnwrappableTokenBufferReadContext(UnwrappableTokenBufferReadContext parent, int type, int index) {
        super(parent, type, index);
    }
    
    public static UnwrappableTokenBufferReadContext createRootContext(JsonStreamContext origContext) {
        // First: possible to have no current context; if so, just create bogus ROOT context
        if (origContext == null) {
            return new UnwrappableTokenBufferReadContext();
        }
        return new UnwrappableTokenBufferReadContext(origContext, ContentReference.unknown());
    }

    public void setNamesToWrap(Set<String> namesToWrap) {
        _namesToWrap = namesToWrap;
    }

    // @since 2.11.1
    public boolean shouldWrap(String localName) {
        return (_namesToWrap != null) && _namesToWrap.contains(localName);
    }
    
    public void setWrapperName(String wrapperName) {
        _wrapperName = wrapperName;
    }
    public String getWrapperName() {
        return _wrapperName;
    }
    public void setWrapperState(int wrapperState) {
        _wrapperState = wrapperState;
    }
    public int getWrapperState() {
        return _wrapperState;
    }

    @Override
    public UnwrappableTokenBufferReadContext createChildArrayContext() {
        // For current context there will be one next Array value, first:
        ++_index;
        return new UnwrappableTokenBufferReadContext(this, TYPE_ARRAY, -1);
    }

    @Override
    public UnwrappableTokenBufferReadContext createChildObjectContext() {
        // For current context there will be one next Object value, first:
        ++_index;
        return new UnwrappableTokenBufferReadContext(this, TYPE_OBJECT, -1);
    }
    
    @Override
    public UnwrappableTokenBufferReadContext parentOrCopy() {
        if (_parent instanceof UnwrappableTokenBufferReadContext) {
            return (UnwrappableTokenBufferReadContext) _parent;
        }
        if (_parent == null) { // unlikely, but just in case let's support
            return new UnwrappableTokenBufferReadContext();
        }
        return new UnwrappableTokenBufferReadContext(_parent, _startLocation);
    }
}
