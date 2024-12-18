package test.extension;

import annotations.RuneAttribute;
import annotations.RuneDataType;
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
import test.extension.B;
import test.extension.B.BBuilder;
import test.extension.B.BBuilderImpl;
import test.extension.B.BImpl;
import test.extension.meta.BMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "@type", visible = true, requireTypeIdForSubtypes = OptBoolean.FALSE)
@RosettaDataType(value="B", builder=B.BBuilderImpl.class, version="0.0.0")
@RuneDataType(value="B", model = "test", builder=B.BBuilderImpl.class, version="0.0.0")
public interface B extends A {

	BMeta metaData = new BMeta();

	/*********************** Getter Methods  ***********************/
	String getFieldB();

	/*********************** Build Methods  ***********************/
	B build();
	
	BBuilder toBuilder();
	
	static BBuilder builder() {
		return new BBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends B> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends B> getType() {
		return B.class;
	}
	
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("fieldA"), String.class, getFieldA(), this);
		processor.processBasic(path.newSubPath("fieldB"), String.class, getFieldB(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BBuilder extends B, A.ABuilder {
		BBuilder setFieldA(String fieldA);
		BBuilder setFieldB(String fieldB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("fieldA"), String.class, getFieldA(), this);
			processor.processBasic(path.newSubPath("fieldB"), String.class, getFieldB(), this);
		}
		

		BBuilder prune();
	}

	/*********************** Immutable Implementation of B  ***********************/
	class BImpl extends A.AImpl implements B {
		private final String fieldB;
		
		protected BImpl(BBuilder builder) {
			super(builder);
			this.fieldB = builder.getFieldB();
		}
		
		@Override
		@RosettaAttribute("fieldB")
		public String getFieldB() {
			return fieldB;
		}
		
		@Override
		public B build() {
			return this;
		}
		
		@Override
		public BBuilder toBuilder() {
			BBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(BBuilder builder) {
			super.setBuilderFields(builder);
			ofNullable(getFieldB()).ifPresent(builder::setFieldB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			B _that = getType().cast(o);
		
			if (!Objects.equals(fieldB, _that.getFieldB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (fieldB != null ? fieldB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "B {" +
				"fieldB=" + this.fieldB +
			'}' + " " + super.toString();
		}
	}

	/*********************** Builder Implementation of B  ***********************/
	class BBuilderImpl extends A.ABuilderImpl  implements BBuilder {
	
		protected String fieldB;
	
		public BBuilderImpl() {
		}
	
		@Override
		@RosettaAttribute("fieldB")
		public String getFieldB() {
			return fieldB;
		}
		
		@Override
		@RosettaAttribute("fieldA")
		public BBuilder setFieldA(String fieldA) {
			this.fieldA = fieldA==null?null:fieldA;
			return this;
		}
		@Override
		@RosettaAttribute("fieldB")
		public BBuilder setFieldB(String fieldB) {
			this.fieldB = fieldB==null?null:fieldB;
			return this;
		}
		
		@Override
		public B build() {
			return new BImpl(this);
		}
		
		@Override
		public BBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public BBuilder prune() {
			super.prune();
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (super.hasData()) return true;
			if (getFieldB()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public BBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			super.merge(other, merger);
			
			BBuilder o = (BBuilder) other;
			
			
			merger.mergeBasic(getFieldB(), o.getFieldB(), this::setFieldB);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
			if (!super.equals(o)) return false;
		
			B _that = getType().cast(o);
		
			if (!Objects.equals(fieldB, _that.getFieldB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = super.hashCode();
			_result = 31 * _result + (fieldB != null ? fieldB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BBuilder {" +
				"fieldB=" + this.fieldB +
			'}' + " " + super.toString();
		}
	}
}
