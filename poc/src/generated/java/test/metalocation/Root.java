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

import test.metalocation.A.ABuilder;
import test.metalocation.Root.RootBuilderImpl;
import test.metalocation.meta.RootMeta;
import test.metalocation.metafields.ReferenceWithMetaB;
import test.metalocation.metafields.ReferenceWithMetaB.ReferenceWithMetaBBuilder;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Root", builder= RootBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Root", model="Just another Rosetta model", builder= RootBuilderImpl.class, version="0.0.0")
public interface Root extends RosettaModelObject {

	RootMeta metaData = new RootMeta();

	/*********************** Getter Methods  ***********************/
	A getTypeA();
	ReferenceWithMetaB getBAddress();

	/*********************** Build Methods  ***********************/
	Root build();
	
	RootBuilder toBuilder();
	
	static RootBuilder builder() {
		return new RootBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends Root> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends Root> getType() {
		return Root.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("typeA"), processor, A.class, getTypeA());
		processRosetta(path.newSubPath("bAddress"), processor, ReferenceWithMetaB.class, getBAddress());
	}
	

	/*********************** Builder Interface  ***********************/
	interface RootBuilder extends Root, RosettaModelObjectBuilder {
		ABuilder getOrCreateTypeA();
		@Override
		ABuilder getTypeA();
		ReferenceWithMetaBBuilder getOrCreateBAddress();
		@Override
		ReferenceWithMetaBBuilder getBAddress();
		RootBuilder setTypeA(A typeA);
		RootBuilder setBAddress(ReferenceWithMetaB bAddress);
		RootBuilder setBAddressValue(B bAddress);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("typeA"), processor, ABuilder.class, getTypeA());
			processRosetta(path.newSubPath("bAddress"), processor, ReferenceWithMetaBBuilder.class, getBAddress());
		}
		

		RootBuilder prune();
	}

	/*********************** Immutable Implementation of Root  ***********************/
	class RootImpl implements Root {
		private final A typeA;
		private final ReferenceWithMetaB bAddress;
		
		protected RootImpl(RootBuilder builder) {
			this.typeA = ofNullable(builder.getTypeA()).map(f->f.build()).orElse(null);
			this.bAddress = ofNullable(builder.getBAddress()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("typeA")
		@RuneAttribute("typeA")
		public A getTypeA() {
			return typeA;
		}
		
		@Override
		@RosettaAttribute("bAddress")
		@RuneAttribute("bAddress")
		public ReferenceWithMetaB getBAddress() {
			return bAddress;
		}
		
		@Override
		public Root build() {
			return this;
		}
		
		@Override
		public RootBuilder toBuilder() {
			RootBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(RootBuilder builder) {
			ofNullable(getTypeA()).ifPresent(builder::setTypeA);
			ofNullable(getBAddress()).ifPresent(builder::setBAddress);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(typeA, _that.getTypeA())) return false;
			if (!Objects.equals(bAddress, _that.getBAddress())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (typeA != null ? typeA.hashCode() : 0);
			_result = 31 * _result + (bAddress != null ? bAddress.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Root {" +
				"typeA=" + this.typeA + ", " +
				"bAddress=" + this.bAddress +
			'}';
		}
	}

	/*********************** Builder Implementation of Root  ***********************/
	class RootBuilderImpl implements RootBuilder {
	
		protected ABuilder typeA;
		protected ReferenceWithMetaBBuilder bAddress;
		
		@Override
		@RosettaAttribute("typeA")
		@RuneAttribute("typeA")
		public ABuilder getTypeA() {
			return typeA;
		}
		
		@Override
		public ABuilder getOrCreateTypeA() {
			ABuilder result;
			if (typeA!=null) {
				result = typeA;
			}
			else {
				result = typeA = A.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("bAddress")
		@RuneAttribute("bAddress")
		public ReferenceWithMetaBBuilder getBAddress() {
			return bAddress;
		}
		
		@Override
		public ReferenceWithMetaBBuilder getOrCreateBAddress() {
			ReferenceWithMetaBBuilder result;
			if (bAddress!=null) {
				result = bAddress;
			}
			else {
				result = bAddress = ReferenceWithMetaB.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("typeA")
		@RuneAttribute("typeA")
		public RootBuilder setTypeA(A _typeA) {
			this.typeA = _typeA == null ? null : _typeA.toBuilder();
			return this;
		}
		
		@Override
		@RosettaAttribute("bAddress")
		@RuneAttribute("bAddress")
		public RootBuilder setBAddress(ReferenceWithMetaB _bAddress) {
			this.bAddress = _bAddress == null ? null : _bAddress.toBuilder();
			return this;
		}
		
		@Override
		public RootBuilder setBAddressValue(B _bAddress) {
			this.getOrCreateBAddress().setValue(_bAddress);
			return this;
		}
		
		@Override
		public Root build() {
			return new RootImpl(this);
		}
		
		@Override
		public RootBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RootBuilder prune() {
			if (typeA!=null && !typeA.prune().hasData()) typeA = null;
			if (bAddress!=null && !bAddress.prune().hasData()) bAddress = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTypeA()!=null && getTypeA().hasData()) return true;
			if (getBAddress()!=null && getBAddress().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RootBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RootBuilder o = (RootBuilder) other;
			
			merger.mergeRosetta(getTypeA(), o.getTypeA(), this::setTypeA);
			merger.mergeRosetta(getBAddress(), o.getBAddress(), this::setBAddress);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(typeA, _that.getTypeA())) return false;
			if (!Objects.equals(bAddress, _that.getBAddress())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (typeA != null ? typeA.hashCode() : 0);
			_result = 31 * _result + (bAddress != null ? bAddress.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RootBuilder {" +
				"typeA=" + this.typeA + ", " +
				"bAddress=" + this.bAddress +
			'}';
		}
	}
}
