package com.regnosys.granite.projector;

import java.util.Objects;
import java.util.Set;

import static com.google.common.base.Preconditions.checkNotNull;

@SuppressWarnings("unused")
public class ProjectionEnvironment {

	public enum FileFormat {
		XML,
		JSON
	}

	private final FileFormat outputFileFormat;
	private final String pojoPackage;
	private final String projectionServiceClass;
	private final Set<String> projectionClassOptions;

	public ProjectionEnvironment(FileFormat outputFileFormat, String pojoPackage, String projectionServiceClass, Set<String> projectionClassOptions) {
		this.outputFileFormat = checkNotNull(outputFileFormat);
		this.pojoPackage = checkNotNull(pojoPackage);
		this.projectionServiceClass = checkNotNull(projectionServiceClass);
		this.projectionClassOptions = checkNotNull(projectionClassOptions);
	}

	public FileFormat getOutputFileFormat() {
		return outputFileFormat;
	}

	public String getPojoPackage() {
		return pojoPackage;
	}

	public String getProjectionServiceClass() {
		return projectionServiceClass;
	}

	public Set<String> getProjectionClassOptions() {
		return projectionClassOptions;
	}

	@Override public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		ProjectionEnvironment that = (ProjectionEnvironment) o;
		return outputFileFormat == that.outputFileFormat &&
			pojoPackage.equals(that.pojoPackage) &&
			projectionServiceClass.equals(that.projectionServiceClass) &&
			projectionClassOptions.equals(that.projectionClassOptions);
	}

	@Override public int hashCode() {
		return Objects.hash(outputFileFormat, pojoPackage, projectionServiceClass, projectionClassOptions);
	}
}
