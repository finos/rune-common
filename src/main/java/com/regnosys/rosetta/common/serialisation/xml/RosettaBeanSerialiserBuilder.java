package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.databind.BeanDescription;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.SerializationConfig;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;
import com.fasterxml.jackson.databind.ser.BeanSerializerBuilder;

public class RosettaBeanSerialiserBuilder extends BeanSerializerBuilder {
    private final static BeanPropertyWriter[] NO_PROPERTIES = new BeanPropertyWriter[0];

    public RosettaBeanSerialiserBuilder(BeanDescription beanDesc) {
        super(beanDesc);
    }

    protected RosettaBeanSerialiserBuilder(RosettaBeanSerialiserBuilder src) {
        super(src);
    }

    @Override
    protected void setConfig(SerializationConfig config) {
        _config = config;
    }

    @Override
    public JsonSerializer<?> build() {
        // [databind#2789]: There can be a case wherein `_typeId` is used, but
        // nothing else. Rare but has happened; so force access.
        if (_typeId != null) {
            if (_config.isEnabled(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
                _typeId.fixAccess(_config.isEnabled(MapperFeature.OVERRIDE_PUBLIC_ACCESS_MODIFIERS));
            }
        }
        if (_anyGetter != null) {
            _anyGetter.fixAccess(_config);
        }

        BeanPropertyWriter[] properties;
        // No properties, any getter or object id writer?
        // No real serializer; caller gets to handle
        if (_properties == null || _properties.isEmpty()) {
            if (_anyGetter == null && _objectIdWriter == null) {
                // NOTE! Caller may still call `createDummy()` later on
                return null;
            }
            properties = NO_PROPERTIES;
        } else {
            properties = _properties.toArray(new BeanPropertyWriter[_properties.size()]);
            if (_config.isEnabled(MapperFeature.CAN_OVERRIDE_ACCESS_MODIFIERS)) {
                for (int i = 0, end = properties.length; i < end; ++i) {
                    properties[i].fixAccess(_config);
                }
            }
        }
        // 27-Apr-2017, tatu: Verify that filtered-properties settings are compatible
        if (_filteredProperties != null) {
            if (_filteredProperties.length != _properties.size()) { // lgtm [java/dereferenced-value-may-be-null]
                throw new IllegalStateException(String.format(
                        "Mismatch between `properties` size (%d), `filteredProperties` (%s): should have as many (or `null` for latter)",
                        _properties.size(), _filteredProperties.length));
            }
        }
        return new RosettaBeanSerialiser(_beanDesc.getType(), this,
                properties, _filteredProperties);
    }
}
