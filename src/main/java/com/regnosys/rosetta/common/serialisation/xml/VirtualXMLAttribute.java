package com.regnosys.rosetta.common.serialisation.xml;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.introspect.TypeResolutionContext;
import com.fasterxml.jackson.databind.introspect.VirtualAnnotatedMember;

public class VirtualXMLAttribute extends VirtualAnnotatedMember {
    public VirtualXMLAttribute(TypeResolutionContext typeContext, Class<?> declaringClass, String name, JavaType type) {

        super(typeContext, declaringClass, name, type);
    }
}
