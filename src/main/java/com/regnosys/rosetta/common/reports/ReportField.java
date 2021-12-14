package com.regnosys.rosetta.common.reports;

import java.util.Comparator;
import java.util.StringJoiner;

public class ReportField implements Comparable<ReportField> {

	private final String name;
	private final String rule;
	private final Integer repeatableIndex;
	private final String value;
	private final String issue;

	public ReportField(String name, String rule, Integer repeatableIndex, String value, String issue) {
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

	@Override public String toString() {
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
