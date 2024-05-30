package com.regnosys.rosetta.common.serialisation.json.preannotation.testpojo;

/*-
 * #%L
 * Rune Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
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
 * #L%
 */

import com.regnosys.rosetta.common.serialisation.json.preannotation.testpojo.metafields.FieldWithMetaPrice;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaClass;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;

import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version test
 */
@RosettaClass
public interface PriceQuantity extends RosettaModelObject {


	/*********************** Getter Methods  ***********************/
	FieldWithMetaPrice getPrice();

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
	default Class<? extends PriceQuantity> getType() {
		return PriceQuantity.class;
	}
	
	
	        @Override
	        default void process(RosettaPath path, Processor processor) {
	        	
	        	processRosetta(path.newSubPath("price"), processor, FieldWithMetaPrice.class, getPrice());
	        }
	        

	/*********************** Builder Interface  ***********************/
	interface PriceQuantityBuilder extends PriceQuantity, RosettaModelObjectBuilder {
		FieldWithMetaPrice.FieldWithMetaPriceBuilder getOrCreatePrice();
		FieldWithMetaPrice.FieldWithMetaPriceBuilder getPrice();
		PriceQuantityBuilder setPrice(FieldWithMetaPrice price0);
		PriceQuantityBuilder setPriceValue(Price price1);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			
			
			processRosetta(path.newSubPath("price"), processor, FieldWithMetaPrice.FieldWithMetaPriceBuilder.class, getPrice());
		}
		

		PriceQuantityBuilder prune();
	}

	/*********************** Immutable Implementation of PriceQuantity  ***********************/
	class PriceQuantityImpl implements PriceQuantity {
		private final FieldWithMetaPrice price;
		
		protected PriceQuantityImpl(PriceQuantityBuilder builder) {
			this.price = ofNullable(builder.getPrice()).map(f->f.build()).orElse(null);
		}
		
		@Override
		public FieldWithMetaPrice getPrice() {
			return price;
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
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			PriceQuantity _that = getType().cast(o);
		
			if (!Objects.equals(price, _that.getPrice())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (price != null ? price.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PriceQuantity {" +
				"price=" + this.price +
			'}';
		}
	}

	/*********************** Builder Implementation of PriceQuantity  ***********************/
	class PriceQuantityBuilderImpl implements PriceQuantityBuilder {
	
		protected FieldWithMetaPrice.FieldWithMetaPriceBuilder price;
	
		public PriceQuantityBuilderImpl() {
		}
	
		@Override
		public FieldWithMetaPrice.FieldWithMetaPriceBuilder getPrice() {
			return price;
		}
		
		@Override
		public FieldWithMetaPrice.FieldWithMetaPriceBuilder getOrCreatePrice() {
			FieldWithMetaPrice.FieldWithMetaPriceBuilder result;
			if (price!=null) {
				result = price;
			}
			else {
				result = price = FieldWithMetaPrice.builder();
				result.getOrCreateMeta().toBuilder().addKey(Key.builder().setScope("DOCUMENT"));
			}
			
			return result;
		}
	
		@Override
		public PriceQuantityBuilder setPrice(FieldWithMetaPrice price) {
			this.price = price==null?null:price.toBuilder();
			return this;
		}
		@Override
		public PriceQuantityBuilder setPriceValue(Price price) {
			this.getOrCreatePrice().setValue(price);
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
			if (price!=null && !price.prune().hasData()) price = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getPrice()!=null && getPrice().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PriceQuantityBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			PriceQuantityBuilder o = (PriceQuantityBuilder) other;
			
			merger.mergeRosetta(getPrice(), o.getPrice(), this::setPrice);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			PriceQuantity _that = getType().cast(o);
		
			if (!Objects.equals(price, _that.getPrice())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (price != null ? price.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PriceQuantityBuilder {" +
				"price=" + this.price +
			'}';
		}
	}
}
