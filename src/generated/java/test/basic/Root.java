package test.basic;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import java.util.Objects;
import test.basic.Root;
import test.basic.Root.RootBuilder;
import test.basic.Root.RootBuilderImpl;
import test.basic.Root.RootImpl;
import test.basic.meta.RootMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Root", builder=Root.RootBuilderImpl.class, version="0.0.0")
public interface Root extends RosettaModelObject {

	RootMeta metaData = new RootMeta();

	/*********************** Getter Methods  ***********************/
	String getStringType();
	String getStringType();

	/*********************** Build Methods  ***********************/
	Root build();
	
	Root.RootBuilder toBuilder();
	
	static Root.RootBuilder builder() {
		return new Root.RootBuilderImpl();
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
		processor.processBasic(path.newSubPath("stringType"), String.class, getStringType(), this);
		processor.processBasic(path.newSubPath("stringType"), String.class, getStringType(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface RootBuilder extends Root, RosettaModelObjectBuilder {
		Root.RootBuilder setStringType(String stringType0);
		Root.RootBuilder setStringType(String stringType1);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("stringType"), String.class, getStringType(), this);
			processor.processBasic(path.newSubPath("stringType"), String.class, getStringType(), this);
		}
		

		Root.RootBuilder prune();
	}

	/*********************** Immutable Implementation of Root  ***********************/
	class RootImpl implements Root {
		private final String stringType0;
		private final String stringType1;
		
		protected RootImpl(Root.RootBuilder builder) {
			this.stringType0 = builder.getStringType();
			this.stringType1 = builder.getStringType();
		}
		
		@Override
		@RosettaAttribute("stringType")
		public String getStringType() {
			return stringType0;
		}
		
		@Override
		@RosettaAttribute("stringType")
		public String getStringType() {
			return stringType1;
		}
		
		@Override
		public Root build() {
			return this;
		}
		
		@Override
		public Root.RootBuilder toBuilder() {
			Root.RootBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(Root.RootBuilder builder) {
			ofNullable(getStringType()).ifPresent(builder::setStringType);
			ofNullable(getStringType()).ifPresent(builder::setStringType);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(stringType0, _that.getStringType())) return false;
			if (!Objects.equals(stringType1, _that.getStringType())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (stringType0 != null ? stringType0.hashCode() : 0);
			_result = 31 * _result + (stringType1 != null ? stringType1.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Root {" +
				"stringType=" + this.stringType0 + ", " +
				"stringType=" + this.stringType1 +
			'}';
		}
	}

	/*********************** Builder Implementation of Root  ***********************/
	class RootBuilderImpl implements Root.RootBuilder {
	
		protected String stringType0;
		protected String stringType1;
	
		public RootBuilderImpl() {
		}
	
		@Override
		@RosettaAttribute("stringType")
		public String getStringType() {
			return stringType0;
		}
		
		@Override
		@RosettaAttribute("stringType")
		public String getStringType() {
			return stringType1;
		}
		
		@Override
		@RosettaAttribute("stringType")
		public Root.RootBuilder setStringType(String stringType0) {
			this.stringType0 = stringType0==null?null:stringType0;
			return this;
		}
		@Override
		@RosettaAttribute("stringType")
		public Root.RootBuilder setStringType(String stringType1) {
			this.stringType1 = stringType1==null?null:stringType1;
			return this;
		}
		
		@Override
		public Root build() {
			return new Root.RootImpl(this);
		}
		
		@Override
		public Root.RootBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Root.RootBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getStringType()!=null) return true;
			if (getStringType()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public Root.RootBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			Root.RootBuilder o = (Root.RootBuilder) other;
			
			
			merger.mergeBasic(getStringType(), o.getStringType(), this::setStringType);
			merger.mergeBasic(getStringType(), o.getStringType(), this::setStringType);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(stringType0, _that.getStringType())) return false;
			if (!Objects.equals(stringType1, _that.getStringType())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (stringType0 != null ? stringType0.hashCode() : 0);
			_result = 31 * _result + (stringType1 != null ? stringType1.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RootBuilder {" +
				"stringType=" + this.stringType0 + ", " +
				"stringType=" + this.stringType1 +
			'}';
		}
	}
}
