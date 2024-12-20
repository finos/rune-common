package test.metakey;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.lib.records.Date;
import com.rosetta.model.metafields.FieldWithMetaDate;
import com.rosetta.model.metafields.FieldWithMetaDate.FieldWithMetaDateBuilder;
import com.rosetta.model.metafields.ReferenceWithMetaDate;
import com.rosetta.model.metafields.ReferenceWithMetaDate.ReferenceWithMetaDateBuilder;
import java.util.Objects;

import test.metakey.AttributeRef.AttributeRefBuilderImpl;
import test.metakey.meta.AttributeRefMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="AttributeRef", builder= AttributeRefBuilderImpl.class, version="0.0.0")
@RuneDataType(value="AttributeRef", model = "test", builder= AttributeRefBuilderImpl.class, version="0.0.0")
public interface AttributeRef extends RosettaModelObject {

	AttributeRefMeta metaData = new AttributeRefMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaDate getDateField();
	ReferenceWithMetaDate getDateReference();

	/*********************** Build Methods  ***********************/
	AttributeRef build();
	
	AttributeRefBuilder toBuilder();
	
	static AttributeRefBuilder builder() {
		return new AttributeRefBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends AttributeRef> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends AttributeRef> getType() {
		return AttributeRef.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("dateField"), processor, FieldWithMetaDate.class, getDateField(), AttributeMeta.GLOBAL_KEY_FIELD);
		processRosetta(path.newSubPath("dateReference"), processor, ReferenceWithMetaDate.class, getDateReference());
	}
	

	/*********************** Builder Interface  ***********************/
	interface AttributeRefBuilder extends AttributeRef, RosettaModelObjectBuilder {
		FieldWithMetaDateBuilder getOrCreateDateField();
		@Override
		FieldWithMetaDateBuilder getDateField();
		ReferenceWithMetaDateBuilder getOrCreateDateReference();
		@Override
		ReferenceWithMetaDateBuilder getDateReference();
		AttributeRefBuilder setDateField(FieldWithMetaDate dateField);
		AttributeRefBuilder setDateFieldValue(Date dateField);
		AttributeRefBuilder setDateReference(ReferenceWithMetaDate dateReference);
		AttributeRefBuilder setDateReferenceValue(Date dateReference);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("dateField"), processor, FieldWithMetaDateBuilder.class, getDateField(), AttributeMeta.GLOBAL_KEY_FIELD);
			processRosetta(path.newSubPath("dateReference"), processor, ReferenceWithMetaDateBuilder.class, getDateReference());
		}
		

		AttributeRefBuilder prune();
	}

	/*********************** Immutable Implementation of AttributeRef  ***********************/
	class AttributeRefImpl implements AttributeRef {
		private final FieldWithMetaDate dateField;
		private final ReferenceWithMetaDate dateReference;
		
		protected AttributeRefImpl(AttributeRefBuilder builder) {
			this.dateField = ofNullable(builder.getDateField()).map(f->f.build()).orElse(null);
			this.dateReference = ofNullable(builder.getDateReference()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("dateField")
		@RuneAttribute("dateField")
		public FieldWithMetaDate getDateField() {
			return dateField;
		}
		
		@Override
		@RosettaAttribute("dateReference")
		@RuneAttribute("dateReference")
		public ReferenceWithMetaDate getDateReference() {
			return dateReference;
		}
		
		@Override
		public AttributeRef build() {
			return this;
		}
		
		@Override
		public AttributeRefBuilder toBuilder() {
			AttributeRefBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(AttributeRefBuilder builder) {
			ofNullable(getDateField()).ifPresent(builder::setDateField);
			ofNullable(getDateReference()).ifPresent(builder::setDateReference);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			AttributeRef _that = getType().cast(o);
		
			if (!Objects.equals(dateField, _that.getDateField())) return false;
			if (!Objects.equals(dateReference, _that.getDateReference())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (dateField != null ? dateField.hashCode() : 0);
			_result = 31 * _result + (dateReference != null ? dateReference.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "AttributeRef {" +
				"dateField=" + this.dateField + ", " +
				"dateReference=" + this.dateReference +
			'}';
		}
	}

	/*********************** Builder Implementation of AttributeRef  ***********************/
	class AttributeRefBuilderImpl implements AttributeRefBuilder {
	
		protected FieldWithMetaDateBuilder dateField;
		protected ReferenceWithMetaDateBuilder dateReference;
		
		@Override
		@RosettaAttribute("dateField")
		@RuneAttribute("dateField")
		public FieldWithMetaDateBuilder getDateField() {
			return dateField;
		}
		
		@Override
		public FieldWithMetaDateBuilder getOrCreateDateField() {
			FieldWithMetaDateBuilder result;
			if (dateField!=null) {
				result = dateField;
			}
			else {
				result = dateField = FieldWithMetaDate.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("dateReference")
		@RuneAttribute("dateReference")
		public ReferenceWithMetaDateBuilder getDateReference() {
			return dateReference;
		}
		
		@Override
		public ReferenceWithMetaDateBuilder getOrCreateDateReference() {
			ReferenceWithMetaDateBuilder result;
			if (dateReference!=null) {
				result = dateReference;
			}
			else {
				result = dateReference = ReferenceWithMetaDate.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("dateField")
		@RuneAttribute("dateField")
		public AttributeRefBuilder setDateField(FieldWithMetaDate _dateField) {
			this.dateField = _dateField == null ? null : _dateField.toBuilder();
			return this;
		}
		
		@Override
		public AttributeRefBuilder setDateFieldValue(Date _dateField) {
			this.getOrCreateDateField().setValue(_dateField);
			return this;
		}
		
		@Override
		@RosettaAttribute("dateReference")
		@RuneAttribute("dateReference")
		public AttributeRefBuilder setDateReference(ReferenceWithMetaDate _dateReference) {
			this.dateReference = _dateReference == null ? null : _dateReference.toBuilder();
			return this;
		}
		
		@Override
		public AttributeRefBuilder setDateReferenceValue(Date _dateReference) {
			this.getOrCreateDateReference().setValue(_dateReference);
			return this;
		}
		
		@Override
		public AttributeRef build() {
			return new AttributeRefImpl(this);
		}
		
		@Override
		public AttributeRefBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public AttributeRefBuilder prune() {
			if (dateField!=null && !dateField.prune().hasData()) dateField = null;
			if (dateReference!=null && !dateReference.prune().hasData()) dateReference = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getDateField()!=null) return true;
			if (getDateReference()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public AttributeRefBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			AttributeRefBuilder o = (AttributeRefBuilder) other;
			
			merger.mergeRosetta(getDateField(), o.getDateField(), this::setDateField);
			merger.mergeRosetta(getDateReference(), o.getDateReference(), this::setDateReference);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			AttributeRef _that = getType().cast(o);
		
			if (!Objects.equals(dateField, _that.getDateField())) return false;
			if (!Objects.equals(dateReference, _that.getDateReference())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (dateField != null ? dateField.hashCode() : 0);
			_result = 31 * _result + (dateReference != null ? dateReference.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "AttributeRefBuilder {" +
				"dateField=" + this.dateField + ", " +
				"dateReference=" + this.dateReference +
			'}';
		}
	}
}
