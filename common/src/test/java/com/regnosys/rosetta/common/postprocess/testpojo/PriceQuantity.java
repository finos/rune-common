package com.regnosys.rosetta.common.postprocess.testpojo;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
 * ==============
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

import com.google.common.collect.ImmutableList;
import com.regnosys.rosetta.common.postprocess.testpojo.metafields.FieldWithMetaPrice;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.*;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.MetaFields;
import com.rosetta.model.metafields.MetaFields.MetaFieldsBuilder;
import com.rosetta.util.ListEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * Defines a settlement as an exchange between two parties of a specified quantity of an asset (the quantity) against a specified quantity of another asset (the price). The settlement is optional and can be either cash or physical. The quantity can additionally be specified in terms of one or more currency amounts. In the case of non-cash products, the settlement of the price/quantity would not be specified here and instead would be delegated to the product mechanics, as parameterised by the price/quantity values.
 * @version 0.0.0.master-SNAPSHOT
 */
@RosettaDataType(value="PriceQuantity", builder= PriceQuantity.PriceQuantityBuilderImpl.class, version="0.0.0.master-SNAPSHOT")
@RuneDataType(value="PriceQuantity", model="cdm", builder= PriceQuantity.PriceQuantityBuilderImpl.class, version="0.0.0.master-SNAPSHOT")
public interface PriceQuantity extends RosettaModelObject, GlobalKey {

	/*********************** Getter Methods  ***********************/
	/**
	 * Specifies a price to be used for trade amounts and other purposes.
	 */
	List<? extends FieldWithMetaPrice> getPrice();

	MetaFields getMeta();

	/*********************** Build Methods  ***********************/
	PriceQuantity build();
	
	PriceQuantityBuilder toBuilder();
	
	static PriceQuantityBuilder builder() {
		return new PriceQuantityBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends PriceQuantity> metaData() {
		return null;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends PriceQuantity> getType() {
		return PriceQuantity.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("price"), processor, FieldWithMetaPrice.class, getPrice());
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface PriceQuantityBuilder extends PriceQuantity, RosettaModelObjectBuilder, GlobalKeyBuilder {
		FieldWithMetaPrice.FieldWithMetaPriceBuilder getOrCreatePrice(int _index);
		@Override
		List<? extends FieldWithMetaPrice.FieldWithMetaPriceBuilder> getPrice();
		MetaFieldsBuilder getOrCreateMeta();
		@Override
		MetaFieldsBuilder getMeta();
		PriceQuantityBuilder addPrice(FieldWithMetaPrice price);
		PriceQuantityBuilder addPrice(FieldWithMetaPrice price, int _idx);
		PriceQuantityBuilder addPriceValue(Price price);
		PriceQuantityBuilder addPriceValue(Price price, int _idx);
		PriceQuantityBuilder addPrice(List<? extends FieldWithMetaPrice> price);
		PriceQuantityBuilder setPrice(List<? extends FieldWithMetaPrice> price);
		PriceQuantityBuilder addPriceValue(List<? extends Price> price);
		PriceQuantityBuilder setPriceValue(List<? extends Price> price);
		PriceQuantityBuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("price"), processor, FieldWithMetaPrice.FieldWithMetaPriceBuilder.class, getPrice());
			processRosetta(path.newSubPath("meta"), processor, MetaFieldsBuilder.class, getMeta());
		}
		

		PriceQuantityBuilder prune();
	}

	/*********************** Immutable Implementation of PriceQuantity  ***********************/
	class PriceQuantityImpl implements PriceQuantity {
		private final List<? extends FieldWithMetaPrice> price;
		private final MetaFields meta;
		
		protected PriceQuantityImpl(PriceQuantityBuilder builder) {
			this.price = ofNullable(builder.getPrice()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("price")
		@RuneAttribute("price")
		@RuneScopedAttributeKey
		public List<? extends FieldWithMetaPrice> getPrice() {
			return price;
		}
		
		@Override
		@RosettaAttribute("meta")
		@RuneAttribute("meta")
		@RuneMetaType
		public MetaFields getMeta() {
			return meta;
		}
		
		@Override
		public PriceQuantity build() {
			return this;
		}
		
		@Override
		public PriceQuantityBuilder toBuilder() {
			PriceQuantityBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(PriceQuantityBuilder builder) {
			ofNullable(getPrice()).ifPresent(builder::setPrice);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			PriceQuantity _that = getType().cast(o);
		
			if (!ListEquals.listEquals(price, _that.getPrice())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (price != null ? price.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PriceQuantity {" +
				"price=" + this.price + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of PriceQuantity  ***********************/
	class PriceQuantityBuilderImpl implements PriceQuantityBuilder {
	
		protected List<FieldWithMetaPrice.FieldWithMetaPriceBuilder> price = new ArrayList<>();
		protected MetaFieldsBuilder meta;
		
		@Override
		@RosettaAttribute("price")
		@RuneAttribute("price")
		@RuneScopedAttributeKey
		public List<? extends FieldWithMetaPrice.FieldWithMetaPriceBuilder> getPrice() {
			return price;
		}
		
		@Override
		public FieldWithMetaPrice.FieldWithMetaPriceBuilder getOrCreatePrice(int _index) {
		
			if (price==null) {
				this.price = new ArrayList<>();
			}
			FieldWithMetaPrice.FieldWithMetaPriceBuilder result;
			return getIndex(price, _index, () -> {
						FieldWithMetaPrice.FieldWithMetaPriceBuilder newPrice = FieldWithMetaPrice.builder();
						newPrice.getOrCreateMeta().addKey(Key.builder().setScope("DOCUMENT"));
						return newPrice;
					});
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
		@RosettaAttribute("price")
		@RuneAttribute("price")
		@RuneScopedAttributeKey
		public PriceQuantityBuilder addPrice(FieldWithMetaPrice _price) {
			if (_price != null) {
				this.price.add(_price.toBuilder());
			}
			return this;
		}
		
		@Override
		public PriceQuantityBuilder addPrice(FieldWithMetaPrice _price, int _idx) {
			getIndex(this.price, _idx, () -> _price.toBuilder());
			return this;
		}
		
		@Override
		public PriceQuantityBuilder addPriceValue(Price _price) {
			this.getOrCreatePrice(-1).setValue(_price.toBuilder());
			return this;
		}
		
		@Override
		public PriceQuantityBuilder addPriceValue(Price _price, int _idx) {
			this.getOrCreatePrice(_idx).setValue(_price.toBuilder());
			return this;
		}
		
		@Override 
		public PriceQuantityBuilder addPrice(List<? extends FieldWithMetaPrice> prices) {
			if (prices != null) {
				for (final FieldWithMetaPrice toAdd : prices) {
					this.price.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@Override 
		@RuneAttribute("price")
		@RuneScopedAttributeKey
		public PriceQuantityBuilder setPrice(List<? extends FieldWithMetaPrice> prices) {
			if (prices == null) {
				this.price = new ArrayList<>();
			} else {
				this.price = prices.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public PriceQuantityBuilder addPriceValue(List<? extends Price> prices) {
			if (prices != null) {
				for (final Price toAdd : prices) {
					this.addPriceValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public PriceQuantityBuilder setPriceValue(List<? extends Price> prices) {
			this.price.clear();
			if (prices != null) {
				prices.forEach(this::addPriceValue);
			}
			return this;
		}
		
		@Override
		@RosettaAttribute("meta")
		@RuneAttribute("meta")
		@RuneMetaType
		public PriceQuantityBuilder setMeta(MetaFields _meta) {
			this.meta = _meta == null ? null : _meta.toBuilder();
			return this;
		}
		
		@Override
		public PriceQuantity build() {
			return new PriceQuantityImpl(this);
		}
		
		@Override
		public PriceQuantityBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PriceQuantityBuilder prune() {
			price = price.stream().filter(b->b!=null).<FieldWithMetaPrice.FieldWithMetaPriceBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getPrice()!=null && getPrice().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PriceQuantityBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			PriceQuantityBuilder o = (PriceQuantityBuilder) other;
			
			merger.mergeRosetta(getPrice(), o.getPrice(), this::getOrCreatePrice);
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			PriceQuantity _that = getType().cast(o);
		
			if (!ListEquals.listEquals(price, _that.getPrice())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (price != null ? price.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PriceQuantityBuilder {" +
				"price=" + this.price + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}
