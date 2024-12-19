package test.extension;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import test.extension.A;
import test.extension.B;
import test.extension.Root;
import test.extension.Root.RootBuilder;
import test.extension.Root.RootBuilderImpl;
import test.extension.Root.RootImpl;
import test.extension.meta.RootMeta;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Root", builder=Root.RootBuilderImpl.class, version="0.0.0")
public interface Root extends RosettaModelObject {

	RootMeta metaData = new RootMeta();

	/*********************** Getter Methods  ***********************/
	A getTypeA();
	B getTypeB();

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
	default Class<? extends Root> getType() {
		return Root.class;
	}
	
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("typeA"), processor, A.class, getTypeA());
		processRosetta(path.newSubPath("typeB"), processor, B.class, getTypeB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface RootBuilder extends Root, RosettaModelObjectBuilder {
		A.ABuilder getOrCreateTypeA();
		A.ABuilder getTypeA();
		B.BBuilder getOrCreateTypeB();
		B.BBuilder getTypeB();
		RootBuilder setTypeA(A typeA);
		RootBuilder setTypeB(B typeB);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("typeA"), processor, A.ABuilder.class, getTypeA());
			processRosetta(path.newSubPath("typeB"), processor, B.BBuilder.class, getTypeB());
		}
		

		RootBuilder prune();
	}

	/*********************** Immutable Implementation of Root  ***********************/
	class RootImpl implements Root {
		private final A typeA;
		private final B typeB;
		
		protected RootImpl(RootBuilder builder) {
			this.typeA = ofNullable(builder.getTypeA()).map(f->f.build()).orElse(null);
			this.typeB = ofNullable(builder.getTypeB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("typeA")
		public A getTypeA() {
			return typeA;
		}
		
		@Override
		@RosettaAttribute("typeB")
		public B getTypeB() {
			return typeB;
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
			ofNullable(getTypeB()).ifPresent(builder::setTypeB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(typeA, _that.getTypeA())) return false;
			if (!Objects.equals(typeB, _that.getTypeB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (typeA != null ? typeA.hashCode() : 0);
			_result = 31 * _result + (typeB != null ? typeB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Root {" +
				"typeA=" + this.typeA + ", " +
				"typeB=" + this.typeB +
			'}';
		}
	}

	/*********************** Builder Implementation of Root  ***********************/
	class RootBuilderImpl implements RootBuilder {
	
		protected A.ABuilder typeA;
		protected B.BBuilder typeB;
	
		public RootBuilderImpl() {
		}
	
		@Override
		@RosettaAttribute("typeA")
		public A.ABuilder getTypeA() {
			return typeA;
		}
		
		@Override
		public A.ABuilder getOrCreateTypeA() {
			A.ABuilder result;
			if (typeA!=null) {
				result = typeA;
			}
			else {
				result = typeA = A.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("typeB")
		public B.BBuilder getTypeB() {
			return typeB;
		}
		
		@Override
		public B.BBuilder getOrCreateTypeB() {
			B.BBuilder result;
			if (typeB!=null) {
				result = typeB;
			}
			else {
				result = typeB = B.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("typeA")
		public RootBuilder setTypeA(A typeA) {
			this.typeA = typeA==null?null:typeA.toBuilder();
			return this;
		}
		@Override
		@RosettaAttribute("typeB")
		public RootBuilder setTypeB(B typeB) {
			this.typeB = typeB==null?null:typeB.toBuilder();
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
			if (typeB!=null && !typeB.prune().hasData()) typeB = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTypeA()!=null && getTypeA().hasData()) return true;
			if (getTypeB()!=null && getTypeB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RootBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RootBuilder o = (RootBuilder) other;
			
			merger.mergeRosetta(getTypeA(), o.getTypeA(), this::setTypeA);
			merger.mergeRosetta(getTypeB(), o.getTypeB(), this::setTypeB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(typeA, _that.getTypeA())) return false;
			if (!Objects.equals(typeB, _that.getTypeB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (typeA != null ? typeA.hashCode() : 0);
			_result = 31 * _result + (typeB != null ? typeB.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RootBuilder {" +
				"typeA=" + this.typeA + ", " +
				"typeB=" + this.typeB +
			'}';
		}
	}
}
