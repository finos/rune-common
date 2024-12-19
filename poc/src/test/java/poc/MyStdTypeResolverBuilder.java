package poc;

import annotations.RuneDataType;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationConfig;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.jsontype.impl.StdTypeResolverBuilder;

public class MyStdTypeResolverBuilder extends StdTypeResolverBuilder {


    public MyStdTypeResolverBuilder(JsonTypeInfo.Value typeInfo) {
        super(typeInfo);
    }

    public MyStdTypeResolverBuilder() {
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
