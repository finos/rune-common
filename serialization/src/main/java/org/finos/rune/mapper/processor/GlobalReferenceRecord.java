package org.finos.rune.mapper.processor;

import java.util.Objects;

public class GlobalReferenceRecord {
    public final Class<?> referenceOnType;
    public final String referenceKeyValue;

    public GlobalReferenceRecord(Class<?> referenceOnType, String referenceKeyValue) {
        this.referenceOnType = referenceOnType;
        this.referenceKeyValue = referenceKeyValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        GlobalReferenceRecord that = (GlobalReferenceRecord) o;
        return Objects.equals(referenceOnType, that.referenceOnType) && Objects.equals(referenceKeyValue, that.referenceKeyValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(referenceOnType, referenceKeyValue);
    }
}
