package test.metascheme.metafields;

import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.annotations.RuneMetaType;
import com.rosetta.model.lib.meta.BasicRosettaMetaData;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.MetaFields;
import com.rosetta.model.metafields.MetaFields.MetaFieldsBuilder;
import java.util.Objects;
import test.metascheme.A;
import test.metascheme.A.ABuilder;

import static java.util.Optional.ofNullable;

/**
 * @version 1
 */
@RosettaDataType(value="FieldWithMetaA", builder=FieldWithMetaA.FieldWithMetaABuilderImpl.class, version="0.0.0")
@RuneDataType(value="FieldWithMetaA", model="Just another Rosetta model", builder=FieldWithMetaA.FieldWithMetaABuilderImpl.class, version="0.0.0")
public interface FieldWithMetaA extends RosettaModelObject, FieldWithMeta<A>, GlobalKey {

	FieldWithMetaAMeta metaData = new FieldWithMetaAMeta();

	/*********************** Getter Methods  ***********************/
	A getValue();
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	FieldWithMetaA build();
	
	FieldWithMetaABuilder toBuilder();
	
	static FieldWithMetaABuilder builder() {
		return new FieldWithMetaABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends FieldWithMetaA> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends FieldWithMetaA> getType() {
		return FieldWithMetaA.class;
	}
	
	@Override
	default Class<A> getValueType() {
		return A.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("value"), processor, A.class, getValue());
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface FieldWithMetaABuilder extends FieldWithMetaA, RosettaModelObjectBuilder, FieldWithMetaBuilder<A>, GlobalKeyBuilder {
		ABuilder getOrCreateValue();
		@Override
		ABuilder getValue();
		MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFieldsBuilder getMeta();
		FieldWithMetaABuilder setValue(A value);
		FieldWithMetaABuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("value"), processor, ABuilder.class, getValue());
			processRosetta(path.newSubPath("meta"), processor, MetaFieldsBuilder.class, getMeta());
		}
		

		FieldWithMetaABuilder prune();
	}

	/*********************** Immutable Implementation of FieldWithMetaA  ***********************/
	class FieldWithMetaAImpl implements FieldWithMetaA {
		private final A value;
		private final MetaFields meta;
		
		protected FieldWithMetaAImpl(FieldWithMetaABuilder builder) {
			this.value = ofNullable(builder.getValue()).map(f->f.build()).orElse(null);
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("value")
		@RuneAttribute("@data")
		public A getValue() {
			return value;
		}
		
		@Override
		@RosettaAttribute("meta")
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields getMeta() {
			return meta;
		}
		
		@Override
		public FieldWithMetaA build() {
			return this;
		}
		
		@Override
		public FieldWithMetaABuilder toBuilder() {
			FieldWithMetaABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(FieldWithMetaABuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaA _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FieldWithMetaA {" +
				"value=" + this.value + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of FieldWithMetaA  ***********************/
	class FieldWithMetaABuilderImpl implements FieldWithMetaABuilder {
	
		protected ABuilder value;
		protected MetaFieldsBuilder meta;
		
		@Override
		@RosettaAttribute("value")
		@RuneAttribute("@data")
		public ABuilder getValue() {
			return value;
		}
		
		@Override
		public ABuilder getOrCreateValue() {
			ABuilder result;
			if (value!=null) {
				result = value;
			}
			else {
				result = value = A.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("meta")
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFieldsBuilder getMeta() {
			return meta;
		}
		
		@Override
		public MetaFieldsBuilder getOrCreateMeta() {
			MetaFieldsBuilder result;
			if (meta!=null) {
				result = meta;
			}
			else {
				result = meta = MetaFields.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("value")
		@RuneAttribute("@data")
		public FieldWithMetaABuilder setValue(A _value) {
			this.value = _value == null ? null : _value.toBuilder();
			return this;
		}
		
		@Override
		@RosettaAttribute("meta")
		@RuneAttribute("meta")
		@RuneMetaType
		public FieldWithMetaABuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public FieldWithMetaA build() {
			return new FieldWithMetaAImpl(this);
		}
		
		@Override
		public FieldWithMetaABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaABuilder prune() {
			if (value!=null && !value.prune().hasData()) value = null;
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getValue()!=null && getValue().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			FieldWithMetaABuilder o = (FieldWithMetaABuilder) other;
			
			merger.mergeRosetta(getValue(), o.getValue(), this::setValue);
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaA _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FieldWithMetaABuilder {" +
				"value=" + this.value + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}

class FieldWithMetaAMeta extends BasicRosettaMetaData<FieldWithMetaA>{

}
