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
import test.metascheme.EnumType;

import static java.util.Optional.ofNullable;

/**
 * @version 1
 */
@RosettaDataType(value="FieldWithMetaEnumType", builder=FieldWithMetaEnumType.FieldWithMetaEnumTypeBuilderImpl.class, version="0.0.0")
@RuneDataType(value="FieldWithMetaEnumType", model="Just another Rosetta model", builder=FieldWithMetaEnumType.FieldWithMetaEnumTypeBuilderImpl.class, version="0.0.0")
public interface FieldWithMetaEnumType extends RosettaModelObject, FieldWithMeta<EnumType>, GlobalKey {

	FieldWithMetaEnumTypeMeta metaData = new FieldWithMetaEnumTypeMeta();

	/*********************** Getter Methods  ***********************/
	EnumType getValue();
	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	FieldWithMetaEnumType build();
	
	FieldWithMetaEnumTypeBuilder toBuilder();
	
	static FieldWithMetaEnumTypeBuilder builder() {
		return new FieldWithMetaEnumTypeBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends FieldWithMetaEnumType> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends FieldWithMetaEnumType> getType() {
		return FieldWithMetaEnumType.class;
	}
	
	@Override
	default Class<EnumType> getValueType() {
		return EnumType.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("value"), EnumType.class, getValue(), this);
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface FieldWithMetaEnumTypeBuilder extends FieldWithMetaEnumType, RosettaModelObjectBuilder, FieldWithMetaBuilder<EnumType>, GlobalKeyBuilder {
		MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFieldsBuilder getMeta();
		FieldWithMetaEnumTypeBuilder setValue(EnumType value);
		FieldWithMetaEnumTypeBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("value"), EnumType.class, getValue(), this);
			processRosetta(path.newSubPath("meta"), processor, MetaFieldsBuilder.class, getMeta());
		}
		

		FieldWithMetaEnumTypeBuilder prune();
	}

	/*********************** Immutable Implementation of FieldWithMetaEnumType  ***********************/
	class FieldWithMetaEnumTypeImpl implements FieldWithMetaEnumType {
		private final EnumType value;
		private final MetaFields meta;
		
		protected FieldWithMetaEnumTypeImpl(FieldWithMetaEnumTypeBuilder builder) {
			this.value = builder.getValue();
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("value")
		@RuneAttribute("@data")
		public EnumType getValue() {
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
		public FieldWithMetaEnumType build() {
			return this;
		}
		
		@Override
		public FieldWithMetaEnumTypeBuilder toBuilder() {
			FieldWithMetaEnumTypeBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(FieldWithMetaEnumTypeBuilder builder) {
			ofNullable(getValue()).ifPresent(builder::setValue);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaEnumType _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FieldWithMetaEnumType {" +
				"value=" + this.value + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of FieldWithMetaEnumType  ***********************/
	class FieldWithMetaEnumTypeBuilderImpl implements FieldWithMetaEnumTypeBuilder {
	
		protected EnumType value;
		protected MetaFieldsBuilder meta;
		
		@Override
		@RosettaAttribute("value")
		@RuneAttribute("@data")
		public EnumType getValue() {
			return value;
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
		public FieldWithMetaEnumTypeBuilder setValue(EnumType _value) {
			this.value = _value == null ? null : _value;
			return this;
		}
		
		@Override
		@RosettaAttribute("meta")
		@RuneAttribute("meta")
		@RuneMetaType
		public FieldWithMetaEnumTypeBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public FieldWithMetaEnumType build() {
			return new FieldWithMetaEnumTypeImpl(this);
		}
		
		@Override
		public FieldWithMetaEnumTypeBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaEnumTypeBuilder prune() {
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getValue()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaEnumTypeBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			FieldWithMetaEnumTypeBuilder o = (FieldWithMetaEnumTypeBuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			merger.mergeBasic(getValue(), o.getValue(), this::setValue);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaEnumType _that = getType().cast(o);
		
			if (!Objects.equals(value, _that.getValue())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (value != null ? value.getClass().getName().hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FieldWithMetaEnumTypeBuilder {" +
				"value=" + this.value + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}

class FieldWithMetaEnumTypeMeta extends BasicRosettaMetaData<FieldWithMetaEnumType>{

}
