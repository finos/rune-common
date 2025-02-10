package org.finos.rune.mapper.processor;

import java.util.Objects;

public class KeyRecord {
    public final Class<?> keyOnType;
    public final String keyReferenceValue;

    public KeyRecord(Class<?> keyOnType, String keyReferenceValue) {
        this.keyOnType = keyOnType;
        this.keyReferenceValue = keyReferenceValue;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        KeyRecord keyRecord = (KeyRecord) o;
        return Objects.equals(keyOnType, keyRecord.keyOnType) && Objects.equals(keyReferenceValue, keyRecord.keyReferenceValue);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyOnType, keyReferenceValue);
    }
}
