package test.metalocation;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import test.metalocation.B.BBuilderImpl;
import test.metalocation.meta.BMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="B", builder= BBuilderImpl.class, version="0.0.0")
@RuneDataType(value="B", model="Just another Rosetta model", builder= BBuilderImpl.class, version="0.0.0")
public interface B extends RosettaModelObject {

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
		processor.processBasic(path.newSubPath("fieldB"), String.class, getFieldB(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BBuilder extends B, RosettaModelObjectBuilder {
		BBuilder setFieldB(String fieldB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("fieldB"), String.class, getFieldB(), this);
		}
		

		BBuilder prune();
	}

	/*********************** Immutable Implementation of B  ***********************/
	class BImpl implements B {
		private final String fieldB;
		
		protected BImpl(BBuilder builder) {
			this.fieldB = builder.getFieldB();
		}
		
		@Override
		@RosettaAttribute("fieldB")
		@RuneAttribute("fieldB")
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
			ofNullable(getFieldB()).ifPresent(builder::setFieldB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			B _that = getType().cast(o);
		
			if (!Objects.equals(fieldB, _that.getFieldB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fieldB != null ? fieldB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "B {" +
				"fieldB=" + this.fieldB +
			'}';
		}
	}

	/*********************** Builder Implementation of B  ***********************/
	class BBuilderImpl implements BBuilder {
	
		protected String fieldB;
		
		@Override
		@RosettaAttribute("fieldB")
		@RuneAttribute("fieldB")
		public String getFieldB() {
			return fieldB;
		}
		
		@Override
		@RosettaAttribute("fieldB")
		@RuneAttribute("fieldB")
		public BBuilder setFieldB(String _fieldB) {
			this.fieldB = _fieldB == null ? null : _fieldB;
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
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getFieldB()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public BBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			BBuilder o = (BBuilder) other;
			
			
			merger.mergeBasic(getFieldB(), o.getFieldB(), this::setFieldB);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			B _that = getType().cast(o);
		
			if (!Objects.equals(fieldB, _that.getFieldB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fieldB != null ? fieldB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BBuilder {" +
				"fieldB=" + this.fieldB +
			'}';
		}
	}
}
