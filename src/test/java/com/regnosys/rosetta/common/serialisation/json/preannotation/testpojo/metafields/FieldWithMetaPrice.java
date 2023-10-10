package com.regnosys.rosetta.common.serialisation.json.preannotation.testpojo.metafields;

import com.regnosys.rosetta.common.serialisation.json.preannotation.testpojo.Price;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaClass;
import com.rosetta.model.lib.meta.BasicRosettaMetaData;
import com.rosetta.model.lib.meta.FieldWithMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version 1
 */
@RosettaClass
public interface FieldWithMetaPrice extends RosettaModelObject, FieldWithMeta<Price>, GlobalKey {

	FieldWithMetaPriceMeta metaData = new FieldWithMetaPriceMeta();

	/*********************** Getter Methods  ***********************/
	MetaFields getMeta();
	Price getValue();

	/*********************** Build Methods  ***********************/
	FieldWithMetaPrice build();
	
	FieldWithMetaPriceBuilder toBuilder();
	
	static FieldWithMetaPriceBuilder builder() {
		return new FieldWithMetaPriceBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends FieldWithMetaPrice> metaData() {
		return metaData;
	}
	
	@Override
	default Class<? extends FieldWithMetaPrice> getType() {
		return FieldWithMetaPrice.class;
	}
	
	@Override
	        default Class<Price> getValueType() {
	            return Price.class;
	        }
	
	        @Override
	        default void process(RosettaPath path, Processor processor) {
	        	
	        	processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	        	processRosetta(path.newSubPath("value"), processor, Price.class, getValue());
	        }
	        

	/*********************** Builder Interface  ***********************/
	interface FieldWithMetaPriceBuilder extends FieldWithMetaPrice, RosettaModelObjectBuilder, GlobalKeyBuilder, FieldWithMetaBuilder<Price> {
		MetaFields.MetaFieldsBuilder getOrCreateMeta();
		MetaFields.MetaFieldsBuilder getMeta();
		Price.PriceBuilder getOrCreateValue();
		Price.PriceBuilder getValue();
		FieldWithMetaPriceBuilder setMeta(MetaFields meta);
		FieldWithMetaPriceBuilder setValue(Price value);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			
			
			processRosetta(path.newSubPath("meta"), processor, MetaFields.MetaFieldsBuilder.class, getMeta());
			processRosetta(path.newSubPath("value"), processor, Price.PriceBuilder.class, getValue());
		}
		

		FieldWithMetaPriceBuilder prune();
	}

	/*********************** Immutable Implementation of FieldWithMetaPrice  ***********************/
	class FieldWithMetaPriceImpl implements FieldWithMetaPrice {
		private final MetaFields meta;
		private final Price value;
		
		protected FieldWithMetaPriceImpl(FieldWithMetaPriceBuilder builder) {
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
			this.value = ofNullable(builder.getValue()).map(f->f.build()).orElse(null);
		}
		
		@Override
		public MetaFields getMeta() {
			return meta;
		}
		
		@Override
		public Price getValue() {
			return value;
		}
		
		@Override
		public FieldWithMetaPrice build() {
			return this;
		}
		
		@Override
		public FieldWithMetaPriceBuilder toBuilder() {
			FieldWithMetaPriceBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(FieldWithMetaPriceBuilder builder) {
			ofNullable(getMeta()).ifPresent(builder::setMeta);
			ofNullable(getValue()).ifPresent(builder::setValue);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaPrice _that = getType().cast(o);
		
			if (!Objects.equals(meta, _that.getMeta())) return false;
			if (!Objects.equals(value, _that.getValue())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FieldWithMetaPrice {" +
				"meta=" + this.meta + ", " +
				"value=" + this.value +
			'}';
		}
	}

	/*********************** Builder Implementation of FieldWithMetaPrice  ***********************/
	class FieldWithMetaPriceBuilderImpl implements FieldWithMetaPriceBuilder {
	
		protected MetaFields.MetaFieldsBuilder meta;
		protected Price.PriceBuilder value;
	
		public FieldWithMetaPriceBuilderImpl() {
		}
	
		@Override
		public MetaFields.MetaFieldsBuilder getMeta() {
			return meta;
		}
		
		@Override
		public MetaFields.MetaFieldsBuilder getOrCreateMeta() {
			MetaFields.MetaFieldsBuilder result;
			if (meta!=null) {
				result = meta;
			}
			else {
				result = meta = MetaFields.builder();
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
		public FieldWithMetaPriceBuilder setMeta(MetaFields meta) {
			this.meta = meta==null?null:meta.toBuilder();
			return this;
		}
		@Override
		public FieldWithMetaPriceBuilder setValue(Price value) {
			this.value = value==null?null:value.toBuilder();
			return this;
		}
		
		@Override
		public FieldWithMetaPrice build() {
			return new FieldWithMetaPriceImpl(this);
		}
		
		@Override
		public FieldWithMetaPriceBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaPriceBuilder prune() {
			if (meta!=null && !meta.prune().hasData()) meta = null;
			if (value!=null && !value.prune().hasData()) value = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getValue()!=null && getValue().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public FieldWithMetaPriceBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			FieldWithMetaPriceBuilder o = (FieldWithMetaPriceBuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			merger.mergeRosetta(getValue(), o.getValue(), this::setValue);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			FieldWithMetaPrice _that = getType().cast(o);
		
			if (!Objects.equals(meta, _that.getMeta())) return false;
			if (!Objects.equals(value, _that.getValue())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			_result = 31 * _result + (value != null ? value.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "FieldWithMetaPriceBuilder {" +
				"meta=" + this.meta + ", " +
				"value=" + this.value +
			'}';
		}
	}
}

class FieldWithMetaPriceMeta extends BasicRosettaMetaData<FieldWithMetaPrice>{

}
