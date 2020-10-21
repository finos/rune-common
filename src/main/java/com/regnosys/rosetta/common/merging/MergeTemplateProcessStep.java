package com.regnosys.rosetta.common.merging;

import com.regnosys.rosetta.common.hashing.SimpleBuilderProcessor;
import com.regnosys.rosetta.common.util.RosettaModelObjectSupplier;
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

public class MergeTemplateProcessStep implements PostProcessStep {

	private static final Logger LOGGER = LoggerFactory.getLogger(MergeTemplateProcessStep.class);

	private final BuilderMerger merger;
	private final RosettaModelObjectSupplier templateSupplier;

	public MergeTemplateProcessStep(BuilderMerger merger, RosettaModelObjectSupplier templateSupplier) {
		this.merger = merger;
		this.templateSupplier = templateSupplier;
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
	public <T extends RosettaModelObject> PostProcessorReport runProcessStep(Class<T> topClass, RosettaModelObjectBuilder builder) {
		MergeTemplateBuilderProcessor process = new MergeTemplateBuilderProcessor();
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		process.processRosetta(path, topClass, builder, null);
		builder.process(path, process);
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
