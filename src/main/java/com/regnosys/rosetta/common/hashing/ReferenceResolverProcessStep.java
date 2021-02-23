package com.regnosys.rosetta.common.hashing;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Table;
import com.regnosys.rosetta.common.util.SimpleBuilderProcessor;
import com.regnosys.rosetta.common.util.SimpleProcessor;
import com.rosetta.lib.postprocess.PostProcessorReport;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.PostProcessStep;
import com.rosetta.model.lib.process.Processor;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

public class ReferenceResolverProcessStep implements PostProcessStep {

	@Override
	public Integer getPriority() {
		return 2;
	}

	@Override
	public String getName() {
		return "Reference Resolver";
	}

	@Override
	public <T extends RosettaModelObject> ReferenceResolverPostProcessorReport runProcessStep(Class<? extends T> topClass,
			T instance) {
		RosettaPath path = RosettaPath.valueOf(topClass.getSimpleName());
		ReferenceCollector referenceCollector = new ReferenceCollector();
		instance.process(path, referenceCollector);
		ReferenceResolver referenceResolver = new ReferenceResolver(referenceCollector.references);
		RosettaModelObjectBuilder builder = instance.toBuilder();
		builder.process(path, referenceResolver);
		referenceResolver.report();
		return new ReferenceResolverPostProcessorReport(referenceCollector.references, builder);
	}

	private static class ReferenceCollector extends SimpleProcessor {

		private final Table<Class<?>, String, Object> references = HashBasedTable.create();

		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType,
				R instance, RosettaModelObject parent, AttributeMeta... metas) {
			if (instance instanceof GlobalKey && instance != null) {
				GlobalKey globalKey = (GlobalKey) instance;
				Object value = getValue(instance);
				Class<?> valueClass = getValueType(instance);
				if (value != null && valueClass != null) {
					ofNullable(globalKey.getMeta())
							.map(GlobalKeyFields::getGlobalKey)
							.ifPresent(gk -> references.put(valueClass, gk, value));
					of(globalKey)
							.map(GlobalKey::getMeta)
							.map(GlobalKeyFields::getKey)
							.ifPresent(keys -> keys.stream()
									.filter(k->k.getKeyValue()!=null)
									.forEach(k -> references.put(valueClass, k.getKeyValue(), value)));
				}
			}
			return true;
		}

		private Object getValue(RosettaModelObject instance) {
			if (instance instanceof FieldWithMeta) {
				return ((FieldWithMeta<?>) instance).getValue();
			} else
				return instance;
		}
		
		private Class<?> getValueType(RosettaModelObject builder) {
			if (builder instanceof FieldWithMeta) {
				return ((FieldWithMeta<?>) builder).getValueType();
			} else
				return builder.getType();
		}

		@Override
		public Report report() {
			return new ReferenceResolverReport(references);
		}
	}

	private static class ReferenceResolver extends SimpleBuilderProcessor {

		private static final RosettaPath LINEAGE_PATH_ELEMENT = RosettaPath.valueOf("lineage");

		private final Table<Class<?>, String, Object> references;

		private ReferenceResolver(Table<Class<?>, String, Object> refs) {
			this.references = refs;
		}

		@SuppressWarnings({ "unchecked", "rawtypes" })
		@Override
		public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<R> rosettaType,
				RosettaModelObjectBuilder builder, RosettaModelObjectBuilder parent, AttributeMeta... metas) {
			if (path.endsWith(LINEAGE_PATH_ELEMENT))
				return false;

			if (builder instanceof ReferenceWithMeta.ReferenceWithMetaBuilder) {
				ReferenceWithMeta.ReferenceWithMetaBuilder referenceWithMetaBuilder = (ReferenceWithMeta.ReferenceWithMetaBuilder) builder;
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
							.map(Entry::getValue)
							.forEach(b -> referenceWithMetaBuilder.setValue(b));
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

	static class ReferenceResolverReport implements BuilderProcessor.Report, Processor.Report {
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
