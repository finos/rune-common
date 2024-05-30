package com.regnosys.rosetta.common.merging;

/*-
 * #%L
 * Rune Common
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

import com.regnosys.rosetta.common.util.RosettaModelObjectSupplier;
import com.regnosys.rosetta.common.util.SimpleBuilderProcessor;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.Templatable;
import com.rosetta.model.lib.meta.TemplateFields;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.PostProcessStep;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Consumer;

public class MergeTemplateProcessStep implements PostProcessStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(MergeTemplateProcessStep.class);

	private final BuilderMerger merger;
	private final RosettaModelObjectSupplier templateSupplier;
	private final Consumer<RosettaModelObjectBuilder> postProcessor;

	public MergeTemplateProcessStep(BuilderMerger merger, RosettaModelObjectSupplier templateSupplier, Consumer<RosettaModelObjectBuilder> postProcessor) {
		this.merger = merger;
		this.templateSupplier = templateSupplier;
		this.postProcessor = postProcessor;
	}

	@Override
	public Integer getPriority() {
		return 1;
	}

	@Override
	public String getName() {
		return "Merge Template Post Processor";
	}

	@Override
	public <T extends RosettaModelObject> PostProcessorReport runProcessStep(Class<? extends T> topClass, T instance) {
		MergeTemplateBuilderProcessor process = new MergeTemplateBuilderProcessor();
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		RosettaModelObjectBuilder builder= instance.toBuilder();
		process.processRosetta(path, topClass, builder, null);
		builder.process(path, process);
		builder.prune();
		// optionally post process
		Optional.ofNullable(postProcessor).ifPresent(p -> p.accept(builder));
		return null;
	}

	private class MergeTemplateBuilderProcessor extends SimpleBuilderProcessor {

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path,
				Class<R> rosettaType,
				RosettaModelObjectBuilder builder,
				RosettaModelObjectBuilder parent,
				AttributeMeta... metas) {

			if (builder == null || !builder.hasData())
				return false;

			getTemplateGlobalReference(builder)
					.flatMap(templateRef -> templateSupplier.get(rosettaType, templateRef))
					.ifPresent(template -> {
						LOGGER.info("Merging {} template with {}", rosettaType.getSimpleName(), merger.getClass().getSimpleName());
						merger.run(builder, template.toBuilder());
					});

			return true;
		}

		@Override
		public Report report() {
			return null;
		}

		private Optional<String> getTemplateGlobalReference(RosettaModelObjectBuilder builder) {
			return Optional.of(builder)
					.filter(Templatable.TemplatableBuilder.class::isInstance)
					.map(Templatable.TemplatableBuilder.class::cast)
					.map(Templatable.TemplatableBuilder::getMeta)
					.map(TemplateFields::getTemplateGlobalReference);
		}
	}
}
