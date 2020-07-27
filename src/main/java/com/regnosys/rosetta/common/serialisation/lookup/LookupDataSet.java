package com.regnosys.rosetta.common.serialisation.lookup;

import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

public class LookupDataSet {

    private String name;
    private String keyType;
    private String valueType;
    private List<LookupDataItem> data;

    public LookupDataSet() {
    }

    public LookupDataSet(String name, String keyType, String valueType, List<LookupDataItem> data) {
        this.name = name;
        this.keyType = keyType;
        this.valueType = valueType;
        this.data = data;
    }

    public String getName() {
        return name;
    }

    public String getKeyType() {
        return keyType;
    }

    public String getValueType() {
        return valueType;
    }

    public List<LookupDataItem> getData() {
        return data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LookupDataSet that = (LookupDataSet) o;
        return Objects.equals(name, that.name) &&
                Objects.equals(keyType, that.keyType) &&
                Objects.equals(valueType, that.valueType) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, keyType, valueType, data);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", LookupDataSet.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("keyType='" + keyType + "'")
                .add("valueType='" + valueType + "'")
                .add("data=" + data)
                .toString();
    }
}
