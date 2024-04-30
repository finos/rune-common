package com.regnosys.rosetta.common.transform;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.regnosys.rosetta.common.translation.Path;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TransformId {
    private final String namespace;
    private final String functionName;
    private final String qualifiedName;

    @JsonCreator
    public TransformId(@JsonProperty("namespace") String namespace,
                       @JsonProperty("functionName") String functionName,
                       @JsonProperty("qualifiedName") String qualifiedName) {
        this.namespace = namespace;
        this.functionName = functionName;
        this.qualifiedName = qualifiedName;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getFunctionName() {
        return functionName;
    }

    public String getQualifiedName() {
        return qualifiedName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TransformId)) return false;
        TransformId that = (TransformId) o;
        return Objects.equals(getNamespace(), that.getNamespace()) && Objects.equals(getFunctionName(), that.getFunctionName()) && Objects.equals(getQualifiedName(), that.getQualifiedName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getNamespace(), getFunctionName(), getQualifiedName());
    }

    public static TransformId valueOf(String qualifiedName) {
        Path path = Path.parse(qualifiedName);
        List<Path.PathElement> pathElements = new ArrayList<>(path.getElements());
        pathElements.remove(pathElements.size() - 1);
        Path namespace = new Path(pathElements);
        return new TransformId(namespace.toString(), path.getLastElement().getPathName(), qualifiedName);
    }

    public static TransformId valueOfAfterStrippingFunctions(String qualifiedNameWithFunction) {
        return valueOf(qualifiedNameWithFunction.replace("functions.", ""));
    }
}
