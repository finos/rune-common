package test.metalocation.metafields;

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
import test.metalocation.B;
import test.metalocation.B.BBuilder;

import static java.util.Optional.ofNullable;

/**
 * @version 1
 */
@RosettaDataType(value="FieldWithMetaB", builder=FieldWithMetaB.FieldWithMetaBBuilderImpl.class, version="0.0.0")
@RuneDataType(value="FieldWithMetaB", model="Just another Rosetta model", builder=FieldWithMetaB.FieldWithMetaBBuilderImpl.class, version="0.0.0")
public interface FieldWithMetaB extends RosettaModelObject, FieldWithMeta<B>, GlobalKey {

	FieldWithMetaBMeta metaData = new FieldWithMetaBMeta();

	/*********************** Getter Methods  ***********************/
	B getValue();
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	FieldWithMetaB build();
	
	FieldWithMetaBBuilder toBuilder();
	
	static FieldWithMetaBBuilder builder() {
		return new FieldWithMetaBBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends FieldWithMetaB> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends FieldWithMetaB> getType() {
		return FieldWithMetaB.class;
	}
	
	@Override
	default Class<B> getValueType() {
		return B.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("value"), processor, B.class, getValue());
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface FieldWithMetaBBuilder extends FieldWithMetaB, RosettaModelObjectBuilder, FieldWithMetaBuilder<B>, GlobalKeyBuilder {
		BBuilder getOrCreateValue();
		@Override
		BBuilder getValue();
		MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFieldsBuilder getMeta();
		FieldWithMetaBBuilder setValue(B value);
		FieldWithMetaBBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("value"), processor, BBuilder.class, getValue());
			processRosetta(path.newSubPath("meta"), processor, MetaFieldsBuilder.class, getMeta());
		}
		

		FieldWithMetaBBuilder prune();
	}

	/*********************** Immutable Implementation of FieldWithMetaB  ***********************/
	class FieldWithMetaBImpl implements FieldWithMetaB {
		private final B value;
		private final MetaFields meta;
		
		protected FieldWithMetaBImpl(FieldWithMetaBBuilder builder) {
			this.value = ofNullable(builder.getValue()).map(f->f.build()).orElse(null);
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("value")
		@RuneAttribute("@data")
		@RuneMetaType
		public B getValue() {
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
		public FieldWithMetaB build() {
			return this;
		}
		
		@Override
		public FieldWithMetaBBuilder toBuilder() {
			FieldWithMetaBBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(FieldWithMetaBBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaB _that = getType().cast(o);
		
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
			return "FieldWithMetaB {" +
				"value=" + this.value + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of FieldWithMetaB  ***********************/
	class FieldWithMetaBBuilderImpl implements FieldWithMetaBBuilder {
	
		protected BBuilder value;
		protected MetaFieldsBuilder meta;
		
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
		@RuneMetaType
		public FieldWithMetaBBuilder setValue(B _value) {
			this.value = _value == null ? null : _value.toBuilder();
			return this;
		}
		
		@Override
		@RosettaAttribute("meta")
		@RuneAttribute("meta")
		@RuneMetaType
		public FieldWithMetaBBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public FieldWithMetaB build() {
			return new FieldWithMetaBImpl(this);
		}
		
		@Override
		public FieldWithMetaBBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaBBuilder prune() {
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
		public FieldWithMetaBBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			FieldWithMetaBBuilder o = (FieldWithMetaBBuilder) other;
			
			merger.mergeRosetta(getValue(), o.getValue(), this::setValue);
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaB _that = getType().cast(o);
		
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
			return "FieldWithMetaBBuilder {" +
				"value=" + this.value + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}

class FieldWithMetaBMeta extends BasicRosettaMetaData<FieldWithMetaB>{

}
