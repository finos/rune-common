package com.regnosys.rosetta.common.serialisation.json.preannotation.testpojo.metafields;

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

import com.regnosys.rosetta.common.serialisation.json.preannotation.testpojo.Price;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaClass;
import com.rosetta.model.lib.meta.BasicRosettaMetaData;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version 1
 */
@RosettaClass
public interface ReferenceWithMetaPrice extends RosettaModelObject, ReferenceWithMeta<Price> {

	ReferenceWithMetaPriceMeta metaData = new ReferenceWithMetaPriceMeta();

	/*********************** Getter Methods  ***********************/
	String getExternalReference();
	String getGlobalReference();
	Reference getReference();
	Price getValue();

	/*********************** Build Methods  ***********************/
	ReferenceWithMetaPrice build();
	
	ReferenceWithMetaPriceBuilder toBuilder();
	
	static ReferenceWithMetaPriceBuilder builder() {
		return new ReferenceWithMetaPriceBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ReferenceWithMetaPrice> metaData() {
		return metaData;
	}
	
	@Override
	default Class<? extends ReferenceWithMetaPrice> getType() {
		return ReferenceWithMetaPrice.class;
	}
	
	@Override
	        default Class<Price> getValueType() {
	            return Price.class;
	        }
	
	        @Override
	        default void process(RosettaPath path, Processor processor) {
	        	processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
	        	processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
	        	
	        	processRosetta(path.newSubPath("reference"), processor, Reference.class, getReference());
	        	processRosetta(path.newSubPath("value"), processor, Price.class, getValue());
	        }
	        

	/*********************** Builder Interface  ***********************/
	interface ReferenceWithMetaPriceBuilder extends ReferenceWithMetaPrice, RosettaModelObjectBuilder, ReferenceWithMetaBuilder<Price> {
		Reference.ReferenceBuilder getOrCreateReference();
		Reference.ReferenceBuilder getReference();
		Price.PriceBuilder getOrCreateValue();
		Price.PriceBuilder getValue();
		ReferenceWithMetaPriceBuilder setExternalReference(String externalReference);
		ReferenceWithMetaPriceBuilder setGlobalReference(String globalReference);
		ReferenceWithMetaPriceBuilder setReference(Reference reference);
		ReferenceWithMetaPriceBuilder setValue(Price value);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			
			processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
			
			processRosetta(path.newSubPath("reference"), processor, Reference.ReferenceBuilder.class, getReference());
			processRosetta(path.newSubPath("value"), processor, Price.PriceBuilder.class, getValue());
		}
		

		ReferenceWithMetaPriceBuilder prune();
	}

	/*********************** Immutable Implementation of ReferenceWithMetaPrice  ***********************/
	class ReferenceWithMetaPriceImpl implements ReferenceWithMetaPrice {
		private final String externalReference;
		private final String globalReference;
		private final Reference reference;
		private final Price value;
		
		protected ReferenceWithMetaPriceImpl(ReferenceWithMetaPriceBuilder builder) {
			this.externalReference = builder.getExternalReference();
			this.globalReference = builder.getGlobalReference();
			this.reference = ofNullable(builder.getReference()).map(f->f.build()).orElse(null);
			this.value = ofNullable(builder.getValue()).map(f->f.build()).orElse(null);
		}
		
		@Override
		public String getExternalReference() {
			return externalReference;
		}
		
		@Override
		public String getGlobalReference() {
			return globalReference;
		}
		
		@Override
		public Reference getReference() {
			return reference;
		}
		
		@Override
		public Price getValue() {
			return value;
		}
		
		@Override
		public ReferenceWithMetaPrice build() {
			return this;
		}
		
		@Override
		public ReferenceWithMetaPriceBuilder toBuilder() {
			ReferenceWithMetaPriceBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ReferenceWithMetaPriceBuilder builder) {
			ofNullable(getExternalReference()).ifPresent(builder::setExternalReference);
			ofNullable(getGlobalReference()).ifPresent(builder::setGlobalReference);
			ofNullable(getReference()).ifPresent(builder::setReference);
			ofNullable(getValue()).ifPresent(builder::setValue);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ReferenceWithMetaPrice _that = getType().cast(o);
		
			if (!Objects.equals(externalReference, _that.getExternalReference())) return false;
			if (!Objects.equals(globalReference, _that.getGlobalReference())) return false;
			if (!Objects.equals(reference, _that.getReference())) return false;
			if (!Objects.equals(value, _that.getValue())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (externalReference != null ? externalReference.hashCode() : 0);
			_result = 31 * _result + (globalReference != null ? globalReference.hashCode() : 0);
			_result = 31 * _result + (reference != null ? reference.hashCode() : 0);
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ReferenceWithMetaPrice {" +
				"externalReference=" + this.externalReference + ", " +
				"globalReference=" + this.globalReference + ", " +
				"reference=" + this.reference + ", " +
				"value=" + this.value +
			'}';
		}
	}

	/*********************** Builder Implementation of ReferenceWithMetaPrice  ***********************/
	class ReferenceWithMetaPriceBuilderImpl implements ReferenceWithMetaPriceBuilder {
	
		protected String externalReference;
		protected String globalReference;
		protected Reference.ReferenceBuilder reference;
		protected Price.PriceBuilder value;
	
		public ReferenceWithMetaPriceBuilderImpl() {
		}
	
		@Override
		public String getExternalReference() {
			return externalReference;
		}
		
		@Override
		public String getGlobalReference() {
			return globalReference;
		}
		
		@Override
		public Reference.ReferenceBuilder getReference() {
			return reference;
		}
		
		@Override
		public Reference.ReferenceBuilder getOrCreateReference() {
			Reference.ReferenceBuilder result;
			if (reference!=null) {
				result = reference;
			}
			else {
				result = reference = Reference.builder();
			}
			
			return result;
		}
		@Override
		public Price.PriceBuilder getValue() {
			return value;
		}
		
		@Override
		public Price.PriceBuilder getOrCreateValue() {
			Price.PriceBuilder result;
			if (value!=null) {
				result = value;
			}
			else {
				result = value = Price.builder();
			}
			
			return result;
		}
	
		@Override
		public ReferenceWithMetaPriceBuilder setExternalReference(String externalReference) {
			this.externalReference = externalReference==null?null:externalReference;
			return this;
		}
		@Override
		public ReferenceWithMetaPriceBuilder setGlobalReference(String globalReference) {
			this.globalReference = globalReference==null?null:globalReference;
			return this;
		}
		@Override
		public ReferenceWithMetaPriceBuilder setReference(Reference reference) {
			this.reference = reference==null?null:reference.toBuilder();
			return this;
		}
		@Override
		public ReferenceWithMetaPriceBuilder setValue(Price value) {
			this.value = value==null?null:value.toBuilder();
			return this;
		}
		
		@Override
		public ReferenceWithMetaPrice build() {
			return new ReferenceWithMetaPriceImpl(this);
		}
		
		@Override
		public ReferenceWithMetaPriceBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ReferenceWithMetaPriceBuilder prune() {
			if (reference!=null && !reference.prune().hasData()) reference = null;
			if (value!=null && !value.prune().hasData()) value = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getExternalReference()!=null) return true;
			if (getGlobalReference()!=null) return true;
			if (getReference()!=null && getReference().hasData()) return true;
			if (getValue()!=null && getValue().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ReferenceWithMetaPriceBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ReferenceWithMetaPriceBuilder o = (ReferenceWithMetaPriceBuilder) other;
			
			merger.mergeRosetta(getReference(), o.getReference(), this::setReference);
			merger.mergeRosetta(getValue(), o.getValue(), this::setValue);
			
			merger.mergeBasic(getExternalReference(), o.getExternalReference(), this::setExternalReference);
			merger.mergeBasic(getGlobalReference(), o.getGlobalReference(), this::setGlobalReference);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ReferenceWithMetaPrice _that = getType().cast(o);
		
			if (!Objects.equals(externalReference, _that.getExternalReference())) return false;
			if (!Objects.equals(globalReference, _that.getGlobalReference())) return false;
			if (!Objects.equals(reference, _that.getReference())) return false;
			if (!Objects.equals(value, _that.getValue())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (externalReference != null ? externalReference.hashCode() : 0);
			_result = 31 * _result + (globalReference != null ? globalReference.hashCode() : 0);
			_result = 31 * _result + (reference != null ? reference.hashCode() : 0);
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ReferenceWithMetaPriceBuilder {" +
				"externalReference=" + this.externalReference + ", " +
				"globalReference=" + this.globalReference + ", " +
				"reference=" + this.reference + ", " +
				"value=" + this.value +
			'}';
		}
	}
}

class ReferenceWithMetaPriceMeta extends BasicRosettaMetaData<ReferenceWithMetaPrice>{

}
