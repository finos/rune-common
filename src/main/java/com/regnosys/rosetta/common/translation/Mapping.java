package com.regnosys.rosetta.common.translation;

import java.util.Objects;

public class Mapping {

    private final Path xmlPath;
    private final Object xmlValue;
    private final Path rosettaPath;
    private final String error;

    public Mapping(Path xmlPath, Object xmlValue, Path rosettaPath, String error) {
        this.xmlPath = xmlPath;
        this.xmlValue = xmlValue;
        this.rosettaPath = rosettaPath;
        this.error = error;
    }

    public Path getXmlPath() {
        return xmlPath;
    }

    public Object getXmlValue() {
        return xmlValue;
    }

    public Path getRosettaPath() {
        return rosettaPath;
    }

    public String getError() {
        return error;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Mapping mapping = (Mapping) o;
        return Objects.equals(xmlPath, mapping.xmlPath) &&
                Objects.equals(xmlValue, mapping.xmlValue) &&
                Objects.equals(rosettaPath, mapping.rosettaPath) &&
                Objects.equals(error, mapping.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(xmlPath, xmlValue, rosettaPath, error);
    }

    @Override
    public String toString() {
        return "Mapping{" +
                "xmlPath=" + xmlPath +
                ", xmlValue=" + xmlValue +
                ", rosettaPath=" + rosettaPath +
                ", error='" + error + '\'' +
                '}';
    }
}
