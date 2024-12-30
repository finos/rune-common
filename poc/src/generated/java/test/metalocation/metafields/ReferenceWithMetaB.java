package test.metalocation.metafields;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneMetaType;
import com.rosetta.model.lib.meta.BasicRosettaMetaData;
import com.rosetta.model.lib.meta.Reference;
import com.rosetta.model.lib.meta.Reference.ReferenceBuilder;
import com.rosetta.model.lib.meta.ReferenceWithMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.metalocation.B;
import test.metalocation.B.BBuilder;

import static java.util.Optional.ofNullable;

/**
 * @version 1
 */
@RosettaDataType(value="ReferenceWithMetaB", builder=ReferenceWithMetaB.ReferenceWithMetaBBuilderImpl.class, version="0.0.0")
@RuneDataType(value="ReferenceWithMetaB", model="Just another Rosetta model", builder=ReferenceWithMetaB.ReferenceWithMetaBBuilderImpl.class, version="0.0.0")
public interface ReferenceWithMetaB extends RosettaModelObject, ReferenceWithMeta<B> {

	ReferenceWithMetaBMeta metaData = new ReferenceWithMetaBMeta();

	/*********************** Getter Methods  ***********************/
	B getValue();
	String getGlobalReference();
	String getExternalReference();
	Reference getReference();

	/*********************** Build Methods  ***********************/
	ReferenceWithMetaB build();
	
	ReferenceWithMetaBBuilder toBuilder();
	
	static ReferenceWithMetaBBuilder builder() {
		return new ReferenceWithMetaBBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ReferenceWithMetaB> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends ReferenceWithMetaB> getType() {
		return ReferenceWithMetaB.class;
	}
	
	@Override
	default Class<B> getValueType() {
		return B.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("value"), processor, B.class, getValue());
		processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
		processRosetta(path.newSubPath("reference"), processor, Reference.class, getReference());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ReferenceWithMetaBBuilder extends ReferenceWithMetaB, RosettaModelObjectBuilder, ReferenceWithMetaBuilder<B> {
		BBuilder getOrCreateValue();
		@Override
		BBuilder getValue();
		ReferenceBuilder getOrCreateReference();
		@Override
		ReferenceBuilder getReference();
		ReferenceWithMetaBBuilder setValue(B value);
		ReferenceWithMetaBBuilder setGlobalReference(String globalReference);
		ReferenceWithMetaBBuilder setExternalReference(String externalReference);
		ReferenceWithMetaBBuilder setReference(Reference reference);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("value"), processor, BBuilder.class, getValue());
			processor.processBasic(path.newSubPath("globalReference"), String.class, getGlobalReference(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("externalReference"), String.class, getExternalReference(), this, AttributeMeta.META);
			processRosetta(path.newSubPath("reference"), processor, ReferenceBuilder.class, getReference());
		}
		

		ReferenceWithMetaBBuilder prune();
	}

	/*********************** Immutable Implementation of ReferenceWithMetaB  ***********************/
	class ReferenceWithMetaBImpl implements ReferenceWithMetaB {
		private final B value;
		private final String globalReference;
		private final String externalReference;
		private final Reference reference;
		
		protected ReferenceWithMetaBImpl(ReferenceWithMetaBBuilder builder) {
			this.value = ofNullable(builder.getValue()).map(f->f.build()).orElse(null);
			this.globalReference = builder.getGlobalReference();
			this.externalReference = builder.getExternalReference();
			this.reference = ofNullable(builder.getReference()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("value")
		@RuneAttribute("@data")
		@RuneMetaType
		public B getValue() {
			return value;
		}
		
		@Override
		@RosettaAttribute("globalReference")
		@RuneAttribute("@ref")
		public String getGlobalReference() {
			return globalReference;
		}
		
		@Override
		@RosettaAttribute("externalReference")
		@RuneAttribute("@ref:external")
		public String getExternalReference() {
			return externalReference;
		}
		
		@Override
		@RosettaAttribute("address")
		@RuneAttribute("@ref:scoped")
		public Reference getReference() {
			return reference;
		}
		
		@Override
		public ReferenceWithMetaB build() {
			return this;
		}
		
		@Override
		public ReferenceWithMetaBBuilder toBuilder() {
			ReferenceWithMetaBBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ReferenceWithMetaBBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getGlobalReference()).ifPresent(builder::setGlobalReference);
			ofNullable(getExternalReference()).ifPresent(builder::setExternalReference);
			ofNullable(getReference()).ifPresent(builder::setReference);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ReferenceWithMetaB _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			if (!Objects.equals(globalReference, _that.getGlobalReference())) return false;
			if (!Objects.equals(externalReference, _that.getExternalReference())) return false;
			if (!Objects.equals(reference, _that.getReference())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			_result = 31 * _result + (globalReference != null ? globalReference.hashCode() : 0);
			_result = 31 * _result + (externalReference != null ? externalReference.hashCode() : 0);
			_result = 31 * _result + (reference != null ? reference.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ReferenceWithMetaB {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}

	/*********************** Builder Implementation of ReferenceWithMetaB  ***********************/
	class ReferenceWithMetaBBuilderImpl implements ReferenceWithMetaBBuilder {
	
		protected BBuilder value;
		protected String globalReference;
		protected String externalReference;
		protected ReferenceBuilder reference;
		
		@Override
		@RosettaAttribute("value")
		@RuneAttribute("@data")
		@RuneMetaType
		public BBuilder getValue() {
			return value;
		}
		
		@Override
		public BBuilder getOrCreateValue() {
			BBuilder result;
			if (value!=null) {
				result = value;
			}
			else {
				result = value = B.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("globalReference")
		@RuneAttribute("@ref")
		public String getGlobalReference() {
			return globalReference;
		}
		
		@Override
		@RosettaAttribute("externalReference")
		@RuneAttribute("@ref:external")
		public String getExternalReference() {
			return externalReference;
		}
		
		@Override
		@RosettaAttribute("address")
		@RuneAttribute("@ref:scoped")
		public ReferenceBuilder getReference() {
			return reference;
		}
		
		@Override
		public ReferenceBuilder getOrCreateReference() {
			ReferenceBuilder result;
			if (reference!=null) {
				result = reference;
			}
			else {
				result = reference = Reference.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("value")
		@RuneAttribute("@data")
		@RuneMetaType
		public ReferenceWithMetaBBuilder setValue(B _value) {
			this.value = _value == null ? null : _value.toBuilder();
			return this;
		}
		
		@Override
		@RosettaAttribute("globalReference")
		@RuneAttribute("@ref")
		public ReferenceWithMetaBBuilder setGlobalReference(String _globalReference) {
			this.globalReference = _globalReference == null ? null : _globalReference;
			return this;
		}
		
		@Override
		@RosettaAttribute("externalReference")
		@RuneAttribute("@ref:external")
		public ReferenceWithMetaBBuilder setExternalReference(String _externalReference) {
			this.externalReference = _externalReference == null ? null : _externalReference;
			return this;
		}
		
		@Override
		@RosettaAttribute("address")
		@RuneAttribute("@ref:scoped")
		public ReferenceWithMetaBBuilder setReference(Reference _reference) {
			this.reference = _reference == null ? null : _reference.toBuilder();
			return this;
		}
		
		@Override
		public ReferenceWithMetaB build() {
			return new ReferenceWithMetaBImpl(this);
		}
		
		@Override
		public ReferenceWithMetaBBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ReferenceWithMetaBBuilder prune() {
			if (value!=null && !value.prune().hasData()) value = null;
			if (reference!=null && !reference.prune().hasData()) reference = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getValue()!=null && getValue().hasData()) return true;
			if (getGlobalReference()!=null) return true;
			if (getExternalReference()!=null) return true;
			if (getReference()!=null && getReference().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ReferenceWithMetaBBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ReferenceWithMetaBBuilder o = (ReferenceWithMetaBBuilder) other;
			
			merger.mergeRosetta(getValue(), o.getValue(), this::setValue);
			merger.mergeRosetta(getReference(), o.getReference(), this::setReference);
			
			merger.mergeBasic(getGlobalReference(), o.getGlobalReference(), this::setGlobalReference);
			merger.mergeBasic(getExternalReference(), o.getExternalReference(), this::setExternalReference);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ReferenceWithMetaB _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			if (!Objects.equals(globalReference, _that.getGlobalReference())) return false;
			if (!Objects.equals(externalReference, _that.getExternalReference())) return false;
			if (!Objects.equals(reference, _that.getReference())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			_result = 31 * _result + (globalReference != null ? globalReference.hashCode() : 0);
			_result = 31 * _result + (externalReference != null ? externalReference.hashCode() : 0);
			_result = 31 * _result + (reference != null ? reference.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ReferenceWithMetaBBuilder {" +
				"value=" + this.value + ", " +
				"globalReference=" + this.globalReference + ", " +
				"externalReference=" + this.externalReference + ", " +
				"reference=" + this.reference +
			'}';
		}
	}
}

class ReferenceWithMetaBMeta extends BasicRosettaMetaData<ReferenceWithMetaB>{

}
