package com.regnosys.rosetta.common.reports;

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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Comparator;
import java.util.StringJoiner;

@Deprecated
public class ReportField implements Comparable<ReportField> {

    private final String name;
    private final String rule;
    private final Integer repeatableIndex;
    private final String value;
    private final String issue;

    @JsonCreator
    public ReportField(@JsonProperty("name") String name,
                       @JsonProperty("rule") String rule,
                       @JsonProperty("repeatableIndex") Integer repeatableIndex,
                       @JsonProperty("value") String value,
                       @JsonProperty("issue") String issue) {
        this.name = name;
        this.rule = rule;
        this.repeatableIndex = repeatableIndex;
        this.value = value;
        this.issue = issue;
    }

    public String getName() {
        return name;
    }

    public String getRule() {
        return rule;
    }

    public Integer getRepeatableIndex() {
        return repeatableIndex;
    }

    public String getValue() {
        return value;
    }

    public String getIssue() {
        return issue;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ReportField.class.getSimpleName() + "[", "]")
                .add("name='" + name + "'")
                .add("rule='" + rule + "'")
                .add("repeatableIndex=" + repeatableIndex)
                .add("value='" + value + "'")
                .add("issue='" + issue + "'")
                .toString();
    }

    @Override
    public int compareTo(ReportField o) {
        return Comparator.comparing(ReportField::getName).compare(this, o);
    }
}
