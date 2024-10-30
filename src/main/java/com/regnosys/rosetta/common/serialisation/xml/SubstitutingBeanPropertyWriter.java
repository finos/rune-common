package com.regnosys.rosetta.common.serialisation.xml;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.io.SerializedString;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

public class SubstitutingBeanPropertyWriter extends BeanPropertyWriter {
    private final SubstitutionMap substitutionNameMap;

    public SubstitutingBeanPropertyWriter(BeanPropertyWriter base, SubstitutionMap substitutionNameMap) {
        super(base);
        this.substitutionNameMap = substitutionNameMap;
    }

    protected SubstitutingBeanPropertyWriter(SubstitutingBeanPropertyWriter base, PropertyName name) {
        super(base, name);
        this.substitutionNameMap = base.substitutionNameMap;
    }

    @Override
    protected SubstitutingBeanPropertyWriter _new(PropertyName newName) {
        return new SubstitutingBeanPropertyWriter(this, newName);
    }

    @Override
    public void serializeAsField(Object bean, JsonGenerator gen,
                                 SerializerProvider prov) throws Exception {
        // inlined 'get()'
        final Object value = (_accessorMethod == null) ? _field.get(bean)
                : _accessorMethod.invoke(bean, (Object[]) null);

        String rawSubstitutedName = substitutionNameMap.getSubstitutedName(value);
        SerializedString substitutedName = rawSubstitutedName == null ? _name : new SerializedString(rawSubstitutedName);

        // Null handling is bit different, check that first
        if (value == null) {
            // 20-Jun-2022, tatu: Defer checking of null, see [databind#3481]
            if((_suppressableValue != null)
                    && prov.includeFilterSuppressNulls(_suppressableValue)) {
                return;
            }
            if (_nullSerializer != null) {
                gen.writeFieldName(substitutedName);
                _nullSerializer.serialize(null, gen, prov);
            }
            return;
        }
        // then find serializer to use
        JsonSerializer<Object> ser = _serializer;
        if (ser == null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap m = _dynamicSerializers;
            ser = m.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(m, cls, prov);
            }
        }
        // and then see if we must suppress certain values (default, empty)
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (ser.isEmpty(prov, value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }
        // For non-nulls: simple check for direct cycles
        if (value == bean) {
            // four choices: exception; handled by call; pass-through or write null
            if (_handleSelfReference(bean, gen, prov, ser)) {
                return;
            }
        }
        gen.writeFieldName(substitutedName);
        if (_typeSerializer == null) {
            ser.serialize(value, gen, prov);
        } else {
            ser.serializeWithType(value, gen, prov, _typeSerializer);
        }
    }
}
