package test.metascheme;

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
import com.rosetta.model.metafields.FieldWithMetaString;
import com.rosetta.model.metafields.FieldWithMetaString.FieldWithMetaStringBuilder;
import java.util.Objects;

import test.metascheme.A.ABuilderImpl;
import test.metascheme.meta.AMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="A", builder= ABuilderImpl.class, version="0.0.0")
@RuneDataType(value="A", model="Just another Rosetta model", builder= ABuilderImpl.class, version="0.0.0")
public interface A extends RosettaModelObject {

	AMeta metaData = new AMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaString getFieldA();

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
		processRosetta(path.newSubPath("fieldA"), processor, FieldWithMetaString.class, getFieldA());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ABuilder extends A, RosettaModelObjectBuilder {
		FieldWithMetaStringBuilder getOrCreateFieldA();
		@Override
		FieldWithMetaStringBuilder getFieldA();
		ABuilder setFieldA(FieldWithMetaString fieldA);
		ABuilder setFieldAValue(String fieldA);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("fieldA"), processor, FieldWithMetaStringBuilder.class, getFieldA());
		}
		

		ABuilder prune();
	}

	/*********************** Immutable Implementation of A  ***********************/
	class AImpl implements A {
		private final FieldWithMetaString fieldA;
		
		protected AImpl(ABuilder builder) {
			this.fieldA = ofNullable(builder.getFieldA()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("fieldA")
		@RuneAttribute("fieldA")
		public FieldWithMetaString getFieldA() {
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
	
		protected FieldWithMetaStringBuilder fieldA;
		
		@Override
		@RosettaAttribute("fieldA")
		@RuneAttribute("fieldA")
		public FieldWithMetaStringBuilder getFieldA() {
			return fieldA;
		}
		
		@Override
		public FieldWithMetaStringBuilder getOrCreateFieldA() {
			FieldWithMetaStringBuilder result;
			if (fieldA!=null) {
				result = fieldA;
			}
			else {
				result = fieldA = FieldWithMetaString.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("fieldA")
		@RuneAttribute("fieldA")
		public ABuilder setFieldA(FieldWithMetaString _fieldA) {
			this.fieldA = _fieldA == null ? null : _fieldA.toBuilder();
			return this;
		}
		
		@Override
		public ABuilder setFieldAValue(String _fieldA) {
			this.getOrCreateFieldA().setValue(_fieldA);
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
			if (fieldA!=null && !fieldA.prune().hasData()) fieldA = null;
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
			
			merger.mergeRosetta(getFieldA(), o.getFieldA(), this::setFieldA);
			
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
