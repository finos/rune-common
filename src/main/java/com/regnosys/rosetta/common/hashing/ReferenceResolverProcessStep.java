package com.regnosys.rosetta.common.hashing;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.GlobalKeyBuilder;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.FieldWithMetaBuilder;
import com.rosetta.model.lib.meta.ReferenceWithMetaBuilder;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.PostProcessStep;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

public class ReferenceResolverProcessStep implements PostProcessStep {

	@Override
	public Integer getPriority() {
		return 3;
	}

	@Override
	public String getName() {
		return "Reference Resolver";
	}

	@Override
	public <T extends RosettaModelObject> ReferenceResolverPostProcessorReport runProcessStep(Class<T> topClass,
			RosettaModelObjectBuilder builder) {
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		ReferenceCollector referenceCollector = new ReferenceCollector();
		builder.process(path, referenceCollector);
		ReferenceResolver referenceResolver = new ReferenceResolver(referenceCollector.references);
		builder.process(path, referenceResolver);
		referenceResolver.report();
		return new ReferenceResolverPostProcessorReport(referenceCollector.references, builder);
	}

	private static class ReferenceCollector extends SimpleBuilderProcessor {

		private final Table<Class<?>, String, Object> references = HashBasedTable.create();

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType,
				RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
			if (builder instanceof GlobalKeyBuilder) {
				GlobalKeyBuilder globalKeyBuilder = (GlobalKeyBuilder) builder;
				Object value = getValue(builder);
				Class<?> valueClass = getValueType(builder);
				ofNullable(globalKeyBuilder.getMeta()).map(m -> m.getGlobalKey())
						.ifPresent(globalKey -> references.put(valueClass, globalKey, value));
				ofNullable(globalKeyBuilder).map(g -> g.getMeta()).map(m -> m.getKeys()).ifPresent(ks -> {
					ks.getKeys().stream().forEach(k -> references.put(valueClass, k.getKeyValue(), value));
				});
			}
			return true;
		}

		private Object getValue(RosettaModelObjectBuilder builder) {
			if (builder instanceof FieldWithMetaBuilder) {
				return ((FieldWithMetaBuilder<?>) builder).getValue();
			}
			else return builder;
		}
		
		private Class<?> getValueType(RosettaModelObjectBuilder builder) {
			if (builder instanceof FieldWithMetaBuilder) {
				return ((FieldWithMetaBuilder<?>) builder).getValueType();
			}
			//TODO this is pretty unpleasant - RosettaModelObjectBuilder should have a getBuiltType method
			else return builder.build().getClass();
		}

		@Override
		public Report report() {
			return new ReferenceResolverReport(references);
		}
	}

	private static class ReferenceResolver extends SimpleBuilderProcessor {

		private final Table<Class<?>, String, Object> references;

		private ReferenceResolver(Table<Class<?>, String, Object> refs) {
			this.references = refs;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType,
				RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
			if (builder instanceof ReferenceWithMetaBuilder) {
				ReferenceWithMetaBuilder referenceWithMetaBuilder = (ReferenceWithMetaBuilder) builder;
				String lookup = null;
				if (referenceWithMetaBuilder.getGlobalReference() != null) {
					lookup = referenceWithMetaBuilder.getGlobalReference();
				}
				else if (referenceWithMetaBuilder.getReference()!=null) {
					lookup = referenceWithMetaBuilder.getReference().getReference();
				}
				
				if (lookup!=null) {
					Map<Class<?>, Object> column = references.column(lookup);
					if (column!=null) {
						List<Entry<Class<?>, Object>> collect = column.entrySet().stream()
							.filter(e->doTest(referenceWithMetaBuilder.getValueType(),e.getKey())).collect(Collectors.toList());
						collect.stream()
							.map(e->e.getValue())
							.map(RosettaModelObjectBuilder.class::cast)
							.forEach(b -> referenceWithMetaBuilder.setValue(b.build()));
					}
				}
			}
			return true;
		}

		private boolean doTest(Class<?> valueType, Class<?> key) {
			return valueType.isAssignableFrom(key);
		}

		@Override
		public Report report() {
			return new ReferenceResolverReport(references);
		}
	}

	static class ReferenceResolverReport implements BuilderProcessor.Report {
		private final Table<Class<?>, String, Object> references;

		private ReferenceResolverReport(Table<Class<?>, String, Object> refs) {
			this.references = refs;
		}

		public Table<Class<?>, String, Object> getReferences() {
			return references;
		}
	}

	public static class ReferenceResolverPostProcessorReport implements PostProcessorReport {
		private final Table<Class<?>, String, Object> references;
		private final RosettaModelObjectBuilder builder;

		private ReferenceResolverPostProcessorReport(Table<Class<?>, String, Object> refs,
				RosettaModelObjectBuilder builder) {
			this.references = refs;
			this.builder = builder;
		}

		public Table<Class<?>, String, Object> getReferences() {
			return this.references;
		}

		@SuppressWarnings("unchecked")
		public <T extends RosettaModelObject> Optional<T> getReferencedObject(Class<T> rosettaType,
				String globalReference) {
			return Optional.ofNullable(references.get(rosettaType, globalReference))
					.map(RosettaModelObjectBuilder.class::cast).map(RosettaModelObjectBuilder::build).map(o -> (T) o);
		}

		@Override
		public RosettaModelObjectBuilder getResultObject() {
			return this.builder;
		}
	}

}
