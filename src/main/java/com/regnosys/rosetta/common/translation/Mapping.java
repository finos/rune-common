package com.regnosys.rosetta.common.translation;

import java.util.Objects;

public class Mapping {

    private final Path xmlPath;
    private final Object xmlValue;
    private final Path rosettaPath;
	private final Object rosettaValue;
    private final String error;
    private final boolean allowsMultiple;
    private final boolean isCondition;

    public Mapping(Path xmlPath, Object xmlValue, Path rosettaPath, Object rosettaValue, String error, boolean allowsMultiple, boolean isCondition) {
        this.xmlPath = xmlPath;
        this.xmlValue = xmlValue;
        this.rosettaPath = rosettaPath;
		this.rosettaValue = rosettaValue;
        this.error = error;
		this.allowsMultiple = allowsMultiple;
		this.isCondition = isCondition;
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

    public Object getRosettaValue() {
		return rosettaValue;
	}

    public String getError() {
        return error;
    }

	public boolean isAllowsMultiple() {
		return allowsMultiple;
	}

	public boolean isCondition() {
		return isCondition;
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
        return String.format("Mapping{xmlPath=%s, xmlValue=%s, rosettaPath=%s, error=%s}",
                xmlPath, xmlValue, rosettaPath, error);
    }
}
