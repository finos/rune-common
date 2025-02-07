package org.finos.rune.mapper.pruning;

public class GlobalReferenceRecord {
    public final Class<?> referenceOnType;
    public final String referenceValue;

    public GlobalReferenceRecord(Class<?> referenceOnType, String referenceValue) {
        this.referenceOnType = referenceOnType;
        this.referenceValue = referenceValue;
    }
}
