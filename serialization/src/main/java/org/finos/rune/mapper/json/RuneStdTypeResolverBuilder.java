package org.finos.rune.mapper.json;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;
import com.rosetta.model.lib.annotations.RuneDataType;

public class RuneStdTypeResolverBuilder extends StdTypeResolverBuilder {

    public RuneStdTypeResolverBuilder(JsonTypeInfo.Value typeInfo) {
        super(typeInfo);
    }

    public RuneStdTypeResolverBuilder() {
        super();
    }

    @Override
    public boolean _strictTypeIdHandling(DeserializationConfig config, JavaType baseType) {
        AnnotatedClass annotatedClass = AnnotatedClassResolver.resolve(config, baseType, config);
        if (annotatedClass.hasAnnotation(RuneDataType.class)) {
            return _requireTypeIdForSubtypes;
        }

        return super._strictTypeIdHandling(config, baseType);
    }
}
