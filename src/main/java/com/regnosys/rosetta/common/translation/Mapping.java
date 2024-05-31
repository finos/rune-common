package com.regnosys.rosetta.common.translation;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
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

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import java.util.Objects;

public class Mapping {

    private final Path xmlPath;
    private final Object xmlValue;
    private Path rosettaPath;
	private Object rosettaValue;
    private String error;
    private final boolean allowsMultiple;
    // mapping for a synonym condition, e.g. set when path = "x->y->z"
    private boolean condition;
    // error if multiple external paths are mapped into a single model path
    private boolean duplicate;

    public Mapping(Path xmlPath, Object xmlValue, Path rosettaPath, Object rosettaValue, String error, boolean allowsMultiple, boolean condition, boolean duplicate) {
        this.xmlPath = xmlPath;
        this.xmlValue = xmlValue;
        this.rosettaPath = rosettaPath;
		this.rosettaValue = rosettaValue;
        this.error = error;
		this.allowsMultiple = allowsMultiple;
		this.condition = condition;
		this.duplicate = duplicate;
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
		return condition;
	}

	public void setCondition(boolean condition) {
		this.condition = condition;
	}

	public boolean isDuplicate() {
		return duplicate;
	}

	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		Mapping mapping = (Mapping) o;
		return allowsMultiple == mapping.allowsMultiple && condition == mapping.condition && duplicate == mapping.duplicate && Objects.equals(xmlPath,
				mapping.xmlPath) && Objects.equals(xmlValue, mapping.xmlValue) && Objects.equals(rosettaPath, mapping.rosettaPath)
				&& Objects.equals(rosettaValue, mapping.rosettaValue) && Objects.equals(error, mapping.error);
	}

	@Override
	public int hashCode() {
		return Objects.hash(xmlPath, xmlValue, rosettaPath, rosettaValue, error, allowsMultiple, condition, duplicate);
	}

	@Override
	public String toString() {
		return "Mapping{" +
				"xmlPath=" + xmlPath +
				", xmlValue=" + xmlValue +
				", rosettaPath=" + rosettaPath +
				", rosettaValue=" + rosettaValue +
				", error='" + error + '\'' +
				", allowsMultiple=" + allowsMultiple +
				", condition=" + condition +
				", duplicate=" + duplicate +
				'}';
	}
}
