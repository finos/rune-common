package test.extension;

import annotations.RuneAttribute;
import annotations.RuneDataType;
import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.OptBoolean;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import test.extension.A;
import test.extension.A.ABuilder;
import test.extension.A.ABuilderImpl;
import test.extension.A.AImpl;
import test.extension.meta.AMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "@type", visible = true, requireTypeIdForSubtypes = OptBoolean.FALSE)
@RosettaDataType(value="A", builder=A.ABuilderImpl.class, version="0.0.0")
@RuneDataType(value="A", model = "test", builder=A.ABuilderImpl.class, version="0.0.0")
@JsonFilter("SubTypeFilter")
public interface A extends RosettaModelObject {

	AMeta metaData = new AMeta();

	/*********************** Getter Methods  ***********************/
	String getFieldA();

	/*********************** Build Methods  ***********************/
	A build();
	
	ABuilder toBuilder();
	
	static ABuilder builder() {
		return new ABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends A> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends A> getType() {
		return A.class;
	}
	
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("fieldA"), String.class, getFieldA(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface ABuilder extends A, RosettaModelObjectBuilder {
		ABuilder setFieldA(String fieldA);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("fieldA"), String.class, getFieldA(), this);
		}
		

		ABuilder prune();
	}

	/*********************** Immutable Implementation of A  ***********************/
	@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "@type", visible = true, requireTypeIdForSubtypes = OptBoolean.FALSE)
	class AImpl implements A {
		private final String fieldA;
		
		protected AImpl(ABuilder builder) {
			this.fieldA = builder.getFieldA();
		}
		
		@Override
		@RosettaAttribute("fieldA")
		public String getFieldA() {
			return fieldA;
		}
		
		@Override
		public A build() {
			return this;
		}
		
		@Override
		public ABuilder toBuilder() {
			ABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ABuilder builder) {
			ofNullable(getFieldA()).ifPresent(builder::setFieldA);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!Objects.equals(fieldA, _that.getFieldA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fieldA != null ? fieldA.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "A {" +
				"fieldA=" + this.fieldA +
			'}';
		}
	}

	/*********************** Builder Implementation of A  ***********************/
	class ABuilderImpl implements ABuilder {
	
		protected String fieldA;
	
		public ABuilderImpl() {
		}
	
		@Override
		@RosettaAttribute("fieldA")
		public String getFieldA() {
			return fieldA;
		}
		
		@Override
		@RosettaAttribute("fieldA")
		public ABuilder setFieldA(String fieldA) {
			this.fieldA = fieldA==null?null:fieldA;
			return this;
		}
		
		@Override
		public A build() {
			return new AImpl(this);
		}
		
		@Override
		public ABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ABuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getFieldA()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ABuilder o = (ABuilder) other;
			
			
			merger.mergeBasic(getFieldA(), o.getFieldA(), this::setFieldA);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!Objects.equals(fieldA, _that.getFieldA())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fieldA != null ? fieldA.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ABuilder {" +
				"fieldA=" + this.fieldA +
			'}';
		}
	}
}
