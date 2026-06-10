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

import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.process.PostProcessor;
import com.rosetta.model.lib.process.ProcessingException;
import com.rosetta.model.lib.validation.ValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Runs an ordered list of {@link PostProcessStep}s against a model object, collecting validation
 * errors, generic errors and per-processor reports.
 */
public class PostProcessorRunner implements PostProcessor {

	private static final Logger LOGGER = LoggerFactory.getLogger(PostProcessorRunner.class);

	private static final String GENERIC_ERROR_TITLE = "Error occurred as part of post processing of ingested document";

	private final List<PostProcessStep> postProcessors;
	private final Map<Class<?>, PostProcessorReport> reportsMap; // not threadsafe

	public PostProcessorRunner(PostProcessStep... postProcessors) {
		this(Arrays.asList(postProcessors));
	}

	public PostProcessorRunner(List<PostProcessStep> postProcessors) {
		this.postProcessors = postProcessors.stream()
				.sorted(Comparator.comparingInt(PostProcessStep::getPriority))
				.collect(Collectors.toList());
		this.reportsMap = new HashMap<>();
	}

	@Override
	public <T extends RosettaModelObject> RosettaModelObjectBuilder postProcess(Class<T> rosettaType,
																				RosettaModelObjectBuilder instance) {
		return postProcess(rosettaType, instance, new ArrayList<>(), new ArrayList<>());
	}

	public <T extends RosettaModelObject> RosettaModelObjectBuilder postProcess(Class<T> rosettaType,
																				RosettaModelObjectBuilder instance,
																				List<ValidationResult<?>> postProcessorErrors,
																				List<GenericErrorReport.ErrorMessage> genericErrors) {
		RosettaModelObjectBuilder obj = instance.prune();
		for (PostProcessStep postProcessor : postProcessors) {
			LOGGER.debug("Running post processor {}", postProcessor.getName());
			try {
				PostProcessorReport report = postProcessor.runProcessStep(rosettaType, obj);
				reportsMap.put(postProcessor.getClass(), report);
			} catch (ProcessingException e) {
				LOGGER.error(e.getLocalizedMessage(), e);
				postProcessorErrors.add(new ValidationResult.ProcessValidationResult<T>(e.getMessage(), e.getObjectName(), e.getProcessorName(), e.getPath()));
			} catch (Exception e) {
				LOGGER.error(e.getLocalizedMessage(), e);
				RosettaPath path = RosettaPath.valueOf(rosettaType.getCanonicalName());
				String value = String.format("The failing Processor \"%s\" in Rosetta Model \"%s\" returned with  error \"%s\"", postProcessor.getName(), path, e.getLocalizedMessage());
				String stackTrace = getStackTraceAsString(e);
				genericErrors.add(new GenericErrorReport.ErrorMessage(GENERIC_ERROR_TITLE, value, stackTrace));
			}
		}

		return obj;
	}

	public Optional<PostProcessorReport> reportFor(Class<?> processor) {
		PostProcessorReport report = reportsMap.get(processor);
		if (report == null) {
			Optional<Class<?>> subClazz = reportsMap.keySet().stream()
					.filter(processor::isAssignableFrom).findFirst();
			if (subClazz.isPresent())
				return reportFor(subClazz.get());
		}
		return Optional.ofNullable(report);
	}

	private String getStackTraceAsString(Exception e) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		e.printStackTrace(pw);
		return sw.toString();
	}
}
