package com.regnosys.rosetta.common.serialisation.preannotation.testpojo;

import com.regnosys.rosetta.common.serialisation.preannotation.testpojo.metafields.ReferenceWithMetaPrice;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaClass;
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
public interface ResolvablePriceQuantity extends RosettaModelObject {


	/*********************** Getter Methods  ***********************/
	ReferenceWithMetaPrice getResolvedPrice();

	/*********************** Build Methods  ***********************/
	ResolvablePriceQuantity build();
	
	ResolvablePriceQuantityBuilder toBuilder();
	
	static ResolvablePriceQuantityBuilder builder() {
		return new ResolvablePriceQuantityBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends ResolvablePriceQuantity> metaData() {
		return null;
	}
	
	@Override
	default Class<? extends ResolvablePriceQuantity> getType() {
		return ResolvablePriceQuantity.class;
	}
	
	
	        @Override
	        default void process(RosettaPath path, Processor processor) {
	        	
	        	processRosetta(path.newSubPath("resolvedPrice"), processor, ReferenceWithMetaPrice.class, getResolvedPrice());
	        }
	        

	/*********************** Builder Interface  ***********************/
	interface ResolvablePriceQuantityBuilder extends ResolvablePriceQuantity, RosettaModelObjectBuilder {
		ReferenceWithMetaPrice.ReferenceWithMetaPriceBuilder getOrCreateResolvedPrice();
		ReferenceWithMetaPrice.ReferenceWithMetaPriceBuilder getResolvedPrice();
		ResolvablePriceQuantityBuilder setResolvedPrice(ReferenceWithMetaPrice resolvedPrice0);
		ResolvablePriceQuantityBuilder setResolvedPriceValue(Price resolvedPrice1);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			
			
			processRosetta(path.newSubPath("resolvedPrice"), processor, ReferenceWithMetaPrice.ReferenceWithMetaPriceBuilder.class, getResolvedPrice());
		}
		

		ResolvablePriceQuantityBuilder prune();
	}

	/*********************** Immutable Implementation of ResolvablePriceQuantity  ***********************/
	class ResolvablePriceQuantityImpl implements ResolvablePriceQuantity {
		private final ReferenceWithMetaPrice resolvedPrice;
		
		protected ResolvablePriceQuantityImpl(ResolvablePriceQuantityBuilder builder) {
			this.resolvedPrice = ofNullable(builder.getResolvedPrice()).map(f->f.build()).orElse(null);
		}
		
		@Override
		public ReferenceWithMetaPrice getResolvedPrice() {
			return resolvedPrice;
		}
		
		@Override
		public ResolvablePriceQuantity build() {
			return this;
		}
		
		@Override
		public ResolvablePriceQuantityBuilder toBuilder() {
			ResolvablePriceQuantityBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ResolvablePriceQuantityBuilder builder) {
			ofNullable(getResolvedPrice()).ifPresent(builder::setResolvedPrice);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ResolvablePriceQuantity _that = getType().cast(o);
		
			if (!Objects.equals(resolvedPrice, _that.getResolvedPrice())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (resolvedPrice != null ? resolvedPrice.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ResolvablePriceQuantity {" +
				"resolvedPrice=" + this.resolvedPrice +
			'}';
		}
	}

	/*********************** Builder Implementation of ResolvablePriceQuantity  ***********************/
	class ResolvablePriceQuantityBuilderImpl implements ResolvablePriceQuantityBuilder {
	
		protected ReferenceWithMetaPrice.ReferenceWithMetaPriceBuilder resolvedPrice;
	
		public ResolvablePriceQuantityBuilderImpl() {
		}
	
		@Override
		public ReferenceWithMetaPrice.ReferenceWithMetaPriceBuilder getResolvedPrice() {
			return resolvedPrice;
		}
		
		@Override
		public ReferenceWithMetaPrice.ReferenceWithMetaPriceBuilder getOrCreateResolvedPrice() {
			ReferenceWithMetaPrice.ReferenceWithMetaPriceBuilder result;
			if (resolvedPrice!=null) {
				result = resolvedPrice;
			}
			else {
				result = resolvedPrice = ReferenceWithMetaPrice.builder();
			}
			
			return result;
		}
	
		@Override
		public ResolvablePriceQuantityBuilder setResolvedPrice(ReferenceWithMetaPrice resolvedPrice) {
			this.resolvedPrice = resolvedPrice==null?null:resolvedPrice.toBuilder();
			return this;
		}
		@Override
		public ResolvablePriceQuantityBuilder setResolvedPriceValue(Price resolvedPrice) {
			this.getOrCreateResolvedPrice().setValue(resolvedPrice);
			return this;
		}
		
		@Override
		public ResolvablePriceQuantity build() {
			return new ResolvablePriceQuantityImpl(this);
		}
		
		@Override
		public ResolvablePriceQuantityBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ResolvablePriceQuantityBuilder prune() {
			if (resolvedPrice!=null && !resolvedPrice.prune().hasData()) resolvedPrice = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getResolvedPrice()!=null && getResolvedPrice().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ResolvablePriceQuantityBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ResolvablePriceQuantityBuilder o = (ResolvablePriceQuantityBuilder) other;
			
			merger.mergeRosetta(getResolvedPrice(), o.getResolvedPrice(), this::setResolvedPrice);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			ResolvablePriceQuantity _that = getType().cast(o);
		
			if (!Objects.equals(resolvedPrice, _that.getResolvedPrice())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (resolvedPrice != null ? resolvedPrice.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ResolvablePriceQuantityBuilder {" +
				"resolvedPrice=" + this.resolvedPrice +
			'}';
		}
	}
}
