package com.regnosys.rosetta.common.serialisation.json.preannotation.testpojo;

/*-
 * #%L
 * Rosetta Common
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

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaClass;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.math.BigDecimal;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version test
 */
@RosettaClass
public interface Price extends RosettaModelObject {

	/*********************** Getter Methods  ***********************/
	BigDecimal getRate();

	/*********************** Build Methods  ***********************/
	Price build();
	
	PriceBuilder toBuilder();
	
	static PriceBuilder builder() {
		return new PriceBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Price> metaData() {
		return null;
	}
	
	@Override
	default Class<? extends Price> getType() {
		return Price.class;
	}
	
	
	        @Override
	        default void process(RosettaPath path, Processor processor) {
	        	processor.processBasic(path.newSubPath("rate"), BigDecimal.class, getRate(), this);
	        	
	        }
	        

	/*********************** Builder Interface  ***********************/
	interface PriceBuilder extends Price, RosettaModelObjectBuilder {
		PriceBuilder setRate(BigDecimal rate);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			
			processor.processBasic(path.newSubPath("rate"), BigDecimal.class, getRate(), this);
			
		}
		

		PriceBuilder prune();
	}

	/*********************** Immutable Implementation of Price  ***********************/
	class PriceImpl implements Price {
		private final BigDecimal rate;
		
		protected PriceImpl(PriceBuilder builder) {
			this.rate = builder.getRate();
		}
		
		@Override
		public BigDecimal getRate() {
			return rate;
		}
		
		@Override
		public Price build() {
			return this;
		}
		
		@Override
		public PriceBuilder toBuilder() {
			PriceBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(PriceBuilder builder) {
			ofNullable(getRate()).ifPresent(builder::setRate);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Price _that = getType().cast(o);
		
			if (!Objects.equals(rate, _that.getRate())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (rate != null ? rate.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Price {" +
				"rate=" + this.rate +
			'}';
		}
	}

	/*********************** Builder Implementation of Price  ***********************/
	class PriceBuilderImpl implements PriceBuilder {
	
		protected BigDecimal rate;
	
		public PriceBuilderImpl() {
		}
	
		@Override
		public BigDecimal getRate() {
			return rate;
		}
		
	
		@Override
		public PriceBuilder setRate(BigDecimal rate) {
			this.rate = rate==null?null:rate;
			return this;
		}
		
		@Override
		public Price build() {
			return new PriceImpl(this);
		}
		
		@Override
		public PriceBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PriceBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getRate()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public PriceBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			PriceBuilder o = (PriceBuilder) other;
			
			
			merger.mergeBasic(getRate(), o.getRate(), this::setRate);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Price _that = getType().cast(o);
		
			if (!Objects.equals(rate, _that.getRate())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (rate != null ? rate.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "PriceBuilder {" +
				"rate=" + this.rate +
			'}';
		}
	}
}
