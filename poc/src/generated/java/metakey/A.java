package metakey;

import annotations.RuneUnwrapped;
import com.rosetta.model.lib.GlobalKey;
import com.rosetta.model.lib.GlobalKey.GlobalKeyBuilder;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.model.metafields.MetaFields;
import java.util.Objects;
import metakey.A;
import metakey.A.ABuilder;
import metakey.A.ABuilderImpl;
import metakey.A.AImpl;
import metakey.meta.AMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="A", builder= ABuilderImpl.class, version="0.0.0")
public interface A extends RosettaModelObject, GlobalKey {

	AMeta metaData = new AMeta();

	/*********************** Getter Methods  ***********************/
	String getFieldA();
	MetaFields getMeta();

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
	default Class<? extends A> getType() {
		return A.class;
	}
	
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("fieldA"), String.class, getFieldA(), this);
		processRosetta(path.newSubPath("meta"), processor, MetaFields.class, getMeta());
	}
	

	/*********************** Builder Interface  ***********************/
	interface ABuilder extends A, RosettaModelObjectBuilder {
		MetaFields.MetaFieldsBuilder getOrCreateMeta();
		MetaFields.MetaFieldsBuilder getMeta();
		ABuilder setFieldA(String fieldA);
		ABuilder setMeta(MetaFields meta);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("fieldA"), String.class, getFieldA(), this);
			processRosetta(path.newSubPath("meta"), processor, MetaFields.MetaFieldsBuilder.class, getMeta());
		}
		

		ABuilder prune();
	}

	/*********************** Immutable Implementation of A  ***********************/
	class AImpl implements A {
		private final String fieldA;
		private final MetaFields meta;
		
		protected AImpl(ABuilder builder) {
			this.fieldA = builder.getFieldA();
			this.meta = ofNullable(builder.getMeta()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("fieldA")
		public String getFieldA() {
			return fieldA;
		}
		
		@Override
		@RosettaAttribute("meta")
		@RuneUnwrapped
		public MetaFields getMeta() {
			return meta;
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
			ofNullable(getFieldA()).ifPresent(builder::setFieldA);
			ofNullable(getMeta()).ifPresent(builder::setMeta);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!Objects.equals(fieldA, _that.getFieldA())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fieldA != null ? fieldA.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "A {" +
				"fieldA=" + this.fieldA + ", " +
				"meta=" + this.meta +
			'}';
		}
	}

	/*********************** Builder Implementation of A  ***********************/
	class ABuilderImpl implements ABuilder, GlobalKeyBuilder {
	
		protected String fieldA;
		protected MetaFields.MetaFieldsBuilder meta;
	
		public ABuilderImpl() {
		}
	
		@Override
		@RosettaAttribute("fieldA")
		public String getFieldA() {
			return fieldA;
		}
		
		@Override
		@RosettaAttribute("meta")
		@RuneUnwrapped
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
		@RosettaAttribute("fieldA")
		public ABuilder setFieldA(String fieldA) {
			this.fieldA = fieldA==null?null:fieldA;
			return this;
		}
		@Override
		@RosettaAttribute("meta")
		@RuneUnwrapped
		public ABuilder setMeta(MetaFields meta) {
			this.meta = meta==null?null:meta.toBuilder();
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
			if (meta!=null && !meta.prune().hasData()) meta = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getFieldA()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public ABuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			ABuilder o = (ABuilder) other;
			
			merger.mergeRosetta(getMeta(), o.getMeta(), this::setMeta);
			
			merger.mergeBasic(getFieldA(), o.getFieldA(), this::setFieldA);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			A _that = getType().cast(o);
		
			if (!Objects.equals(fieldA, _that.getFieldA())) return false;
			if (!Objects.equals(meta, _that.getMeta())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (fieldA != null ? fieldA.hashCode() : 0);
			_result = 31 * _result + (meta != null ? meta.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "ABuilder {" +
				"fieldA=" + this.fieldA + ", " +
				"meta=" + this.meta +
			'}';
		}
	}
}
