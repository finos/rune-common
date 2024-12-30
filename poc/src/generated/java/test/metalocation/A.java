package test.metalocation;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.annotations.RuneAttribute;
import com.rosetta.model.lib.annotations.RuneDataType;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;

import test.metalocation.A.ABuilderImpl;
import test.metalocation.meta.AMeta;
import test.metalocation.metafields.FieldWithMetaB;
import test.metalocation.metafields.FieldWithMetaB.FieldWithMetaBBuilder;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="A", builder= ABuilderImpl.class, version="0.0.0")
@RuneDataType(value="A", model="Just another Rosetta model", builder= ABuilderImpl.class, version="0.0.0")
public interface A extends RosettaModelObject {

	AMeta metaData = new AMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaB getB();

	/*********************** Build Methods  ***********************/
	A build();
	
	ABuilder toBuilder();
	
	static ABuilder builder() {
		return new ABuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends A> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends A> getType() {
		return A.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("b"), processor, FieldWithMetaB.class, getB());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ABuilder extends A, RosettaModelObjectBuilder {
		FieldWithMetaBBuilder getOrCreateB();
		@Override
		FieldWithMetaBBuilder getB();
		ABuilder setB(FieldWithMetaB b);
		ABuilder setBValue(B b);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("b"), processor, FieldWithMetaBBuilder.class, getB());
		}
		

		ABuilder prune();
	}

	/*********************** Immutable Implementation of A  ***********************/
	class AImpl implements A {
		private final FieldWithMetaB b;
		
		protected AImpl(ABuilder builder) {
			this.b = ofNullable(builder.getB()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("b")
		@RuneAttribute("b")
		public FieldWithMetaB getB() {
			return b;
		}
		
		@Override
		public A build() {
			return this;
		}
		
		@Override
		public ABuilder toBuilder() {
			ABuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(ABuilder builder) {
			ofNullable(getB()).ifPresent(builder::setB);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!Objects.equals(b, _that.getB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "A {" +
				"b=" + this.b +
			'}';
		}
	}

	/*********************** Builder Implementation of A  ***********************/
	class ABuilderImpl implements ABuilder {
	
		protected FieldWithMetaBBuilder b;
		
		@Override
		@RosettaAttribute("b")
		@RuneAttribute("b")
		public FieldWithMetaBBuilder getB() {
			return b;
		}
		
		@Override
		public FieldWithMetaBBuilder getOrCreateB() {
			FieldWithMetaBBuilder result;
			if (b!=null) {
				result = b;
			}
			else {
				result = b = FieldWithMetaB.builder();
				result.getOrCreateMeta().toBuilder().addKey(Key.builder().setScope("DOCUMENT"));
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("b")
		@RuneAttribute("b")
		public ABuilder setB(FieldWithMetaB _b) {
			this.b = _b == null ? null : _b.toBuilder();
			return this;
		}
		
		@Override
		public ABuilder setBValue(B _b) {
			this.getOrCreateB().setValue(_b);
			return this;
		}
		
		@Override
		public A build() {
			return new AImpl(this);
		}
		
		@Override
		public ABuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ABuilder prune() {
			if (b!=null && !b.prune().hasData()) b = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getB()!=null && getB().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ABuilder o = (ABuilder) other;
			
			merger.mergeRosetta(getB(), o.getB(), this::setB);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!Objects.equals(b, _that.getB())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (b != null ? b.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ABuilder {" +
				"b=" + this.b +
			'}';
		}
	}
}
