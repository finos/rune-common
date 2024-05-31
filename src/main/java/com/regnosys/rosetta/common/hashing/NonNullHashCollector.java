package com.regnosys.rosetta.common.hashing;

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

import com.regnosys.rosetta.common.util.SimpleProcessor;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.meta.TemplateFields;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.Processor;

import java.util.Arrays;
import java.util.Optional;

/**
 * A simple implementation of {@link Processor} that only
 * considers non-null values. For all non-null primitive values it uses the
 * accumulate method of the integer report to accumulate a hashcode
 */
public class NonNullHashCollector extends SimpleProcessor {

	private final IntegerHashGenerator hashcodeGenerator;
	protected final IntegerReport report;
	private static final RosettaPath EXTERNAL_REFERENCE_PATH_ELEMENT = RosettaPath.valueOf("externalReference");

	public NonNullHashCollector() {
		this.hashcodeGenerator = new IntegerHashGenerator();
		report = new IntegerReport(0);
	}

	@Override
	public <R extends RosettaModelObject> boolean processRosetta(RosettaPath path, Class<? extends R> rosettaType, R instance,
			RosettaModelObject parent, AttributeMeta... metas) {
		Result result = shouldIncludeInHash(instance, parent, metas);
		if (result.includeInHash) {
			report.accumulate();
		}
		return result.continueProcessing;
	}

	@Override
	public <T> void processBasic(RosettaPath path, Class<? extends T> rosettaType, T instance, RosettaModelObject parent,
			AttributeMeta... metas) {
		if (instance != null
				&& (!metaContains(metas, AttributeMeta.META) || isExternalKeyOrReference(path, parent, metas))) {
			int hash = hashcodeGenerator.generate(instance);
			report.accumulate(hash);
		}

	}

	@Override
	public IntegerReport report() {
		return report;
	}

	private boolean isExternalKeyOrReference(RosettaPath path, RosettaModelObject parent,
			AttributeMeta[] metas) {
		return metaContains(metas, AttributeMeta.EXTERNAL_KEY) || (ReferenceWithMeta.class.isInstance(parent)
				&& path.endsWith(EXTERNAL_REFERENCE_PATH_ELEMENT));
	}

	/**
	 * Should include in hash if: - metas is empty and we don't have a
	 * MetaFieldsBuilder - Its a regular attribute that we need to hash - Have a
	 * MetaFieldsBuilder and an IS_META meta attribute - This is meta attribute we
	 * want to hash like scheme
	 */
	private Result shouldIncludeInHash(RosettaModelObject instance, RosettaModelObject parent,
			AttributeMeta[] metas) {
		if (instance == null || !instance.toBuilder().hasData()) {
			return new Result(false, false);
		}
		if (isGlobalKeyFields(instance)) {
			// do not include meta folder in hash, however it's contents maybe included
			return new Result(false, true);
		}
		if (isTemplateFields(instance)) {
			// do not include template folder in hash
			return new Result(false, false);
		}
		if (isReferenceWithMeta(instance)) {
			// do not include reference folder in hash, however it's contents maybe included
			// (e.g if it's a value with no reference)
			return new Result(false, true);
		}
		if (isReferenceWithMetaContainingReference(parent)) {
			// if the parent contains a reference, don't include any children in the hash
			// (and stop processing)
			return new Result(false, false);
		}
		if (metaContains(metas, AttributeMeta.GLOBAL_KEY) || metaContains(metas, AttributeMeta.GLOBAL_KEY_FIELD)) {
			return new Result(false, false);
		}
		return new Result(true, true);
	}

	private boolean isReferenceWithMetaContainingReference(RosettaModelObject instance) {
		if (isReferenceWithMeta(instance)) {
			ReferenceWithMeta<?> refBuilder = (ReferenceWithMeta<?>) instance;
			return Optional.ofNullable(refBuilder.getReference()).isPresent();
		}
		return false;
	}

	private boolean isReferenceWithMeta(RosettaModelObject instance) {
		return instance instanceof ReferenceWithMeta
				&& ((ReferenceWithMeta<?>) instance).getReference() != null;
	}

	private boolean isGlobalKeyFields(RosettaModelObject instance) {
		return instance instanceof GlobalKeyFields;
	}

	private boolean isTemplateFields(RosettaModelObject instance) {
		return instance instanceof TemplateFields;
	}

	private boolean metaContains(AttributeMeta[] metas, AttributeMeta attributeMeta) {
		return Arrays.stream(metas).anyMatch(m -> m == attributeMeta);
	}

	private static class Result {
		private final boolean includeInHash;
		private final boolean continueProcessing;

		public Result(boolean includeInHash, boolean continueProcessing) {
			this.includeInHash = includeInHash;
			this.continueProcessing = continueProcessing;
		}

		@Override
		public String toString() {
			return "Result{" + "includeInHash=" + includeInHash + ", continueProcessing=" + continueProcessing + '}';
		}
	}
}
