package test.basic;

import test.basic.BasicSingle.BasicSingleBuilderImpl;
import test.basic.meta.BasicSingleMeta;
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
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.Objects;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="BasicSingle", builder= BasicSingleBuilderImpl.class, version="0.0.0")
@RuneDataType(value="BasicSingle", model="test", builder= BasicSingleBuilderImpl.class, version="0.0.0")
public interface BasicSingle extends RosettaModelObject {

	BasicSingleMeta metaData = new BasicSingleMeta();

	/*********************** Getter Methods  ***********************/
	Boolean getBooleanType();
	BigDecimal getNumberType();
	BigDecimal getParameterisedNumberType();
	String getParameterisedStringType();
	String getStringType();
	LocalTime getTimeType();

	/*********************** Build Methods  ***********************/
	BasicSingle build();
	
	BasicSingleBuilder toBuilder();
	
	static BasicSingleBuilder builder() {
		return new BasicSingleBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends BasicSingle> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends BasicSingle> getType() {
		return BasicSingle.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("booleanType"), Boolean.class, getBooleanType(), this);
		processor.processBasic(path.newSubPath("numberType"), BigDecimal.class, getNumberType(), this);
		processor.processBasic(path.newSubPath("parameterisedNumberType"), BigDecimal.class, getParameterisedNumberType(), this);
		processor.processBasic(path.newSubPath("parameterisedStringType"), String.class, getParameterisedStringType(), this);
		processor.processBasic(path.newSubPath("stringType"), String.class, getStringType(), this);
		processor.processBasic(path.newSubPath("timeType"), LocalTime.class, getTimeType(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BasicSingleBuilder extends BasicSingle, RosettaModelObjectBuilder {
		BasicSingleBuilder setBooleanType(Boolean booleanType);
		BasicSingleBuilder setNumberType(BigDecimal numberType);
		BasicSingleBuilder setParameterisedNumberType(BigDecimal parameterisedNumberType);
		BasicSingleBuilder setParameterisedStringType(String parameterisedStringType);
		BasicSingleBuilder setStringType(String stringType);
		BasicSingleBuilder setTimeType(LocalTime timeType);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("booleanType"), Boolean.class, getBooleanType(), this);
			processor.processBasic(path.newSubPath("numberType"), BigDecimal.class, getNumberType(), this);
			processor.processBasic(path.newSubPath("parameterisedNumberType"), BigDecimal.class, getParameterisedNumberType(), this);
			processor.processBasic(path.newSubPath("parameterisedStringType"), String.class, getParameterisedStringType(), this);
			processor.processBasic(path.newSubPath("stringType"), String.class, getStringType(), this);
			processor.processBasic(path.newSubPath("timeType"), LocalTime.class, getTimeType(), this);
		}
		

		BasicSingleBuilder prune();
	}

	/*********************** Immutable Implementation of BasicSingle  ***********************/
	class BasicSingleImpl implements BasicSingle {
		private final Boolean booleanType;
		private final BigDecimal numberType;
		private final BigDecimal parameterisedNumberType;
		private final String parameterisedStringType;
		private final String stringType;
		private final LocalTime timeType;
		
		protected BasicSingleImpl(BasicSingleBuilder builder) {
			this.booleanType = builder.getBooleanType();
			this.numberType = builder.getNumberType();
			this.parameterisedNumberType = builder.getParameterisedNumberType();
			this.parameterisedStringType = builder.getParameterisedStringType();
			this.stringType = builder.getStringType();
			this.timeType = builder.getTimeType();
		}
		
		@Override
		@RosettaAttribute("booleanType")
		@RuneAttribute("booleanType")
		public Boolean getBooleanType() {
			return booleanType;
		}
		
		@Override
		@RosettaAttribute("numberType")
		@RuneAttribute("numberType")
		public BigDecimal getNumberType() {
			return numberType;
		}
		
		@Override
		@RosettaAttribute("parameterisedNumberType")
		@RuneAttribute("parameterisedNumberType")
		public BigDecimal getParameterisedNumberType() {
			return parameterisedNumberType;
		}
		
		@Override
		@RosettaAttribute("parameterisedStringType")
		@RuneAttribute("parameterisedStringType")
		public String getParameterisedStringType() {
			return parameterisedStringType;
		}
		
		@Override
		@RosettaAttribute("stringType")
		@RuneAttribute("stringType")
		public String getStringType() {
			return stringType;
		}
		
		@Override
		@RosettaAttribute("timeType")
		@RuneAttribute("timeType")
		public LocalTime getTimeType() {
			return timeType;
		}
		
		@Override
		public BasicSingle build() {
			return this;
		}
		
		@Override
		public BasicSingleBuilder toBuilder() {
			BasicSingleBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(BasicSingleBuilder builder) {
			ofNullable(getBooleanType()).ifPresent(builder::setBooleanType);
			ofNullable(getNumberType()).ifPresent(builder::setNumberType);
			ofNullable(getParameterisedNumberType()).ifPresent(builder::setParameterisedNumberType);
			ofNullable(getParameterisedStringType()).ifPresent(builder::setParameterisedStringType);
			ofNullable(getStringType()).ifPresent(builder::setStringType);
			ofNullable(getTimeType()).ifPresent(builder::setTimeType);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			BasicSingle _that = getType().cast(o);
		
			if (!Objects.equals(booleanType, _that.getBooleanType())) return false;
			if (!Objects.equals(numberType, _that.getNumberType())) return false;
			if (!Objects.equals(parameterisedNumberType, _that.getParameterisedNumberType())) return false;
			if (!Objects.equals(parameterisedStringType, _that.getParameterisedStringType())) return false;
			if (!Objects.equals(stringType, _that.getStringType())) return false;
			if (!Objects.equals(timeType, _that.getTimeType())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (booleanType != null ? booleanType.hashCode() : 0);
			_result = 31 * _result + (numberType != null ? numberType.hashCode() : 0);
			_result = 31 * _result + (parameterisedNumberType != null ? parameterisedNumberType.hashCode() : 0);
			_result = 31 * _result + (parameterisedStringType != null ? parameterisedStringType.hashCode() : 0);
			_result = 31 * _result + (stringType != null ? stringType.hashCode() : 0);
			_result = 31 * _result + (timeType != null ? timeType.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BasicSingle {" +
				"booleanType=" + this.booleanType + ", " +
				"numberType=" + this.numberType + ", " +
				"parameterisedNumberType=" + this.parameterisedNumberType + ", " +
				"parameterisedStringType=" + this.parameterisedStringType + ", " +
				"stringType=" + this.stringType + ", " +
				"timeType=" + this.timeType +
			'}';
		}
	}

	/*********************** Builder Implementation of BasicSingle  ***********************/
	class BasicSingleBuilderImpl implements BasicSingleBuilder {
	
		protected Boolean booleanType;
		protected BigDecimal numberType;
		protected BigDecimal parameterisedNumberType;
		protected String parameterisedStringType;
		protected String stringType;
		protected LocalTime timeType;
		
		@Override
		@RosettaAttribute("booleanType")
		@RuneAttribute("booleanType")
		public Boolean getBooleanType() {
			return booleanType;
		}
		
		@Override
		@RosettaAttribute("numberType")
		@RuneAttribute("numberType")
		public BigDecimal getNumberType() {
			return numberType;
		}
		
		@Override
		@RosettaAttribute("parameterisedNumberType")
		@RuneAttribute("parameterisedNumberType")
		public BigDecimal getParameterisedNumberType() {
			return parameterisedNumberType;
		}
		
		@Override
		@RosettaAttribute("parameterisedStringType")
		@RuneAttribute("parameterisedStringType")
		public String getParameterisedStringType() {
			return parameterisedStringType;
		}
		
		@Override
		@RosettaAttribute("stringType")
		@RuneAttribute("stringType")
		public String getStringType() {
			return stringType;
		}
		
		@Override
		@RosettaAttribute("timeType")
		@RuneAttribute("timeType")
		public LocalTime getTimeType() {
			return timeType;
		}
		
		@Override
		@RosettaAttribute("booleanType")
		@RuneAttribute("booleanType")
		public BasicSingleBuilder setBooleanType(Boolean _booleanType) {
			this.booleanType = _booleanType == null ? null : _booleanType;
			return this;
		}
		
		@Override
		@RosettaAttribute("numberType")
		@RuneAttribute("numberType")
		public BasicSingleBuilder setNumberType(BigDecimal _numberType) {
			this.numberType = _numberType == null ? null : _numberType;
			return this;
		}
		
		@Override
		@RosettaAttribute("parameterisedNumberType")
		@RuneAttribute("parameterisedNumberType")
		public BasicSingleBuilder setParameterisedNumberType(BigDecimal _parameterisedNumberType) {
			this.parameterisedNumberType = _parameterisedNumberType == null ? null : _parameterisedNumberType;
			return this;
		}
		
		@Override
		@RosettaAttribute("parameterisedStringType")
		@RuneAttribute("parameterisedStringType")
		public BasicSingleBuilder setParameterisedStringType(String _parameterisedStringType) {
			this.parameterisedStringType = _parameterisedStringType == null ? null : _parameterisedStringType;
			return this;
		}
		
		@Override
		@RosettaAttribute("stringType")
		@RuneAttribute("stringType")
		public BasicSingleBuilder setStringType(String _stringType) {
			this.stringType = _stringType == null ? null : _stringType;
			return this;
		}
		
		@Override
		@RosettaAttribute("timeType")
		@RuneAttribute("timeType")
		public BasicSingleBuilder setTimeType(LocalTime _timeType) {
			this.timeType = _timeType == null ? null : _timeType;
			return this;
		}
		
		@Override
		public BasicSingle build() {
			return new BasicSingleImpl(this);
		}
		
		@Override
		public BasicSingleBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public BasicSingleBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBooleanType()!=null) return true;
			if (getNumberType()!=null) return true;
			if (getParameterisedNumberType()!=null) return true;
			if (getParameterisedStringType()!=null) return true;
			if (getStringType()!=null) return true;
			if (getTimeType()!=null) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public BasicSingleBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			BasicSingleBuilder o = (BasicSingleBuilder) other;
			
			
			merger.mergeBasic(getBooleanType(), o.getBooleanType(), this::setBooleanType);
			merger.mergeBasic(getNumberType(), o.getNumberType(), this::setNumberType);
			merger.mergeBasic(getParameterisedNumberType(), o.getParameterisedNumberType(), this::setParameterisedNumberType);
			merger.mergeBasic(getParameterisedStringType(), o.getParameterisedStringType(), this::setParameterisedStringType);
			merger.mergeBasic(getStringType(), o.getStringType(), this::setStringType);
			merger.mergeBasic(getTimeType(), o.getTimeType(), this::setTimeType);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			BasicSingle _that = getType().cast(o);
		
			if (!Objects.equals(booleanType, _that.getBooleanType())) return false;
			if (!Objects.equals(numberType, _that.getNumberType())) return false;
			if (!Objects.equals(parameterisedNumberType, _that.getParameterisedNumberType())) return false;
			if (!Objects.equals(parameterisedStringType, _that.getParameterisedStringType())) return false;
			if (!Objects.equals(stringType, _that.getStringType())) return false;
			if (!Objects.equals(timeType, _that.getTimeType())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (booleanType != null ? booleanType.hashCode() : 0);
			_result = 31 * _result + (numberType != null ? numberType.hashCode() : 0);
			_result = 31 * _result + (parameterisedNumberType != null ? parameterisedNumberType.hashCode() : 0);
			_result = 31 * _result + (parameterisedStringType != null ? parameterisedStringType.hashCode() : 0);
			_result = 31 * _result + (stringType != null ? stringType.hashCode() : 0);
			_result = 31 * _result + (timeType != null ? timeType.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BasicSingleBuilder {" +
				"booleanType=" + this.booleanType + ", " +
				"numberType=" + this.numberType + ", " +
				"parameterisedNumberType=" + this.parameterisedNumberType + ", " +
				"parameterisedStringType=" + this.parameterisedStringType + ", " +
				"stringType=" + this.stringType + ", " +
				"timeType=" + this.timeType +
			'}';
		}
	}
}
