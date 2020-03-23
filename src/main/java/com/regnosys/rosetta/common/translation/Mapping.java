package com.regnosys.rosetta.common.translation;

import java.util.Objects;

public class Mapping {

    private final Path xmlPath;
    private final Object xmlValue;
    private Path rosettaPath;
	private Object rosettaValue;
    private String error;
    private final boolean allowsMultiple;
    private boolean isCondition;

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

	public void setRosettaPath(Path rosettaPath) {
		this.rosettaPath = rosettaPath;
	}

	public Object getRosettaValue() {
		return rosettaValue;
	}

	public void setRosettaValue(Object rosettaValue) {
		this.rosettaValue = rosettaValue;
	}

	public String getError() {
        return error;
    }

	public void setError(String error) {
		this.error = error;
	}

	public boolean isAllowsMultiple() {
		return allowsMultiple;
	}

	public boolean isCondition() {
		return isCondition;
	}

	public void setCondition(boolean condition) {
		isCondition = condition;
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
        return String.format("Mapping{xmlPath=%s, xmlValue=%s, rosettaPath=%s, rosettaValue=%s, error=%s}",
                xmlPath, xmlValue, rosettaPath, rosettaValue, error);
    }
}
