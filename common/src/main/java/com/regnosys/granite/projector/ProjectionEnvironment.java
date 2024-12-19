package com.regnosys.granite.projector;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

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
