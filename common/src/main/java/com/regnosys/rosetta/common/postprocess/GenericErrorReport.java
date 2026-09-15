package com.regnosys.rosetta.common.postprocess;

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

import java.util.List;

/**
 * Collects generic (non-validation) error messages produced while running post-processing steps.
 */
public class GenericErrorReport {
	private final List<ErrorMessage> errors;

	public GenericErrorReport(List<ErrorMessage> errors) {
		this.errors = errors;
	}

	public List<ErrorMessage> getErrors() {
		return errors;
	}

	public static class ErrorMessage {

		private final String title;
		private final String value;
		private final String stackTrace;

		public ErrorMessage(String title, String value, String stackTrace) {
			this.title = title;
			this.value = value;
			this.stackTrace = stackTrace;
		}

		public String getTitle() {
			return title;
		}

		public String getValue() {
			return value;
		}

		public String getStackTrace() {
			return stackTrace;
		}
	}
}
