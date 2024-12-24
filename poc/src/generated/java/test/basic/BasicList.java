package test.basic;

import test.basic.BasicList.BasicListBuilderImpl;
import test.basic.meta.BasicListMeta;
import com.google.common.collect.ImmutableList;
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
import com.rosetta.util.ListEquals;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="BasicList", builder= BasicListBuilderImpl.class, version="0.0.0")
@RuneDataType(value="BasicList", model="test", builder= BasicListBuilderImpl.class, version="0.0.0")
public interface BasicList extends RosettaModelObject {

	BasicListMeta metaData = new BasicListMeta();

	/*********************** Getter Methods  ***********************/
	List<Boolean> getBooleanTypes();
	List<BigDecimal> getNumberTypes();
	List<BigDecimal> getParameterisedNumberTypes();
	List<String> getStringTypes();
	List<LocalTime> getTimeTypes();

	/*********************** Build Methods  ***********************/
	BasicList build();
	
	BasicListBuilder toBuilder();
	
	static BasicListBuilder builder() {
		return new BasicListBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends BasicList> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends BasicList> getType() {
		return BasicList.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("booleanTypes"), Boolean.class, getBooleanTypes(), this);
		processor.processBasic(path.newSubPath("numberTypes"), BigDecimal.class, getNumberTypes(), this);
		processor.processBasic(path.newSubPath("parameterisedNumberTypes"), BigDecimal.class, getParameterisedNumberTypes(), this);
		processor.processBasic(path.newSubPath("stringTypes"), String.class, getStringTypes(), this);
		processor.processBasic(path.newSubPath("timeTypes"), LocalTime.class, getTimeTypes(), this);
	}
	

	/*********************** Builder Interface  ***********************/
	interface BasicListBuilder extends BasicList, RosettaModelObjectBuilder {
		BasicListBuilder addBooleanTypes(Boolean booleanTypes);
		BasicListBuilder addBooleanTypes(Boolean booleanTypes, int _idx);
		BasicListBuilder addBooleanTypes(List<Boolean> booleanTypes);
		BasicListBuilder setBooleanTypes(List<Boolean> booleanTypes);
		BasicListBuilder addNumberTypes(BigDecimal numberTypes);
		BasicListBuilder addNumberTypes(BigDecimal numberTypes, int _idx);
		BasicListBuilder addNumberTypes(List<BigDecimal> numberTypes);
		BasicListBuilder setNumberTypes(List<BigDecimal> numberTypes);
		BasicListBuilder addParameterisedNumberTypes(BigDecimal parameterisedNumberTypes);
		BasicListBuilder addParameterisedNumberTypes(BigDecimal parameterisedNumberTypes, int _idx);
		BasicListBuilder addParameterisedNumberTypes(List<BigDecimal> parameterisedNumberTypes);
		BasicListBuilder setParameterisedNumberTypes(List<BigDecimal> parameterisedNumberTypes);
		BasicListBuilder addStringTypes(String stringTypes);
		BasicListBuilder addStringTypes(String stringTypes, int _idx);
		BasicListBuilder addStringTypes(List<String> stringTypes);
		BasicListBuilder setStringTypes(List<String> stringTypes);
		BasicListBuilder addTimeTypes(LocalTime timeTypes);
		BasicListBuilder addTimeTypes(LocalTime timeTypes, int _idx);
		BasicListBuilder addTimeTypes(List<LocalTime> timeTypes);
		BasicListBuilder setTimeTypes(List<LocalTime> timeTypes);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("booleanTypes"), Boolean.class, getBooleanTypes(), this);
			processor.processBasic(path.newSubPath("numberTypes"), BigDecimal.class, getNumberTypes(), this);
			processor.processBasic(path.newSubPath("parameterisedNumberTypes"), BigDecimal.class, getParameterisedNumberTypes(), this);
			processor.processBasic(path.newSubPath("stringTypes"), String.class, getStringTypes(), this);
			processor.processBasic(path.newSubPath("timeTypes"), LocalTime.class, getTimeTypes(), this);
		}
		

		BasicListBuilder prune();
	}

	/*********************** Immutable Implementation of BasicList  ***********************/
	class BasicListImpl implements BasicList {
		private final List<Boolean> booleanTypes;
		private final List<BigDecimal> numberTypes;
		private final List<BigDecimal> parameterisedNumberTypes;
		private final List<String> stringTypes;
		private final List<LocalTime> timeTypes;
		
		protected BasicListImpl(BasicListBuilder builder) {
			this.booleanTypes = ofNullable(builder.getBooleanTypes()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.numberTypes = ofNullable(builder.getNumberTypes()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.parameterisedNumberTypes = ofNullable(builder.getParameterisedNumberTypes()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.stringTypes = ofNullable(builder.getStringTypes()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
			this.timeTypes = ofNullable(builder.getTimeTypes()).filter(_l->!_l.isEmpty()).map(ImmutableList::copyOf).orElse(null);
		}
		
		@Override
		@RosettaAttribute("booleanTypes")
		@RuneAttribute("booleanTypes")
		public List<Boolean> getBooleanTypes() {
			return booleanTypes;
		}
		
		@Override
		@RosettaAttribute("numberTypes")
		@RuneAttribute("numberTypes")
		public List<BigDecimal> getNumberTypes() {
			return numberTypes;
		}
		
		@Override
		@RosettaAttribute("parameterisedNumberTypes")
		@RuneAttribute("parameterisedNumberTypes")
		public List<BigDecimal> getParameterisedNumberTypes() {
			return parameterisedNumberTypes;
		}
		
		@Override
		@RosettaAttribute("stringTypes")
		@RuneAttribute("stringTypes")
		public List<String> getStringTypes() {
			return stringTypes;
		}
		
		@Override
		@RosettaAttribute("timeTypes")
		@RuneAttribute("timeTypes")
		public List<LocalTime> getTimeTypes() {
			return timeTypes;
		}
		
		@Override
		public BasicList build() {
			return this;
		}
		
		@Override
		public BasicListBuilder toBuilder() {
			BasicListBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(BasicListBuilder builder) {
			ofNullable(getBooleanTypes()).ifPresent(builder::setBooleanTypes);
			ofNullable(getNumberTypes()).ifPresent(builder::setNumberTypes);
			ofNullable(getParameterisedNumberTypes()).ifPresent(builder::setParameterisedNumberTypes);
			ofNullable(getStringTypes()).ifPresent(builder::setStringTypes);
			ofNullable(getTimeTypes()).ifPresent(builder::setTimeTypes);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			BasicList _that = getType().cast(o);
		
			if (!ListEquals.listEquals(booleanTypes, _that.getBooleanTypes())) return false;
			if (!ListEquals.listEquals(numberTypes, _that.getNumberTypes())) return false;
			if (!ListEquals.listEquals(parameterisedNumberTypes, _that.getParameterisedNumberTypes())) return false;
			if (!ListEquals.listEquals(stringTypes, _that.getStringTypes())) return false;
			if (!ListEquals.listEquals(timeTypes, _that.getTimeTypes())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (booleanTypes != null ? booleanTypes.hashCode() : 0);
			_result = 31 * _result + (numberTypes != null ? numberTypes.hashCode() : 0);
			_result = 31 * _result + (parameterisedNumberTypes != null ? parameterisedNumberTypes.hashCode() : 0);
			_result = 31 * _result + (stringTypes != null ? stringTypes.hashCode() : 0);
			_result = 31 * _result + (timeTypes != null ? timeTypes.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BasicList {" +
				"booleanTypes=" + this.booleanTypes + ", " +
				"numberTypes=" + this.numberTypes + ", " +
				"parameterisedNumberTypes=" + this.parameterisedNumberTypes + ", " +
				"stringTypes=" + this.stringTypes + ", " +
				"timeTypes=" + this.timeTypes +
			'}';
		}
	}

	/*********************** Builder Implementation of BasicList  ***********************/
	class BasicListBuilderImpl implements BasicListBuilder {
	
		protected List<Boolean> booleanTypes = new ArrayList<>();
		protected List<BigDecimal> numberTypes = new ArrayList<>();
		protected List<BigDecimal> parameterisedNumberTypes = new ArrayList<>();
		protected List<String> stringTypes = new ArrayList<>();
		protected List<LocalTime> timeTypes = new ArrayList<>();
		
		@Override
		@RosettaAttribute("booleanTypes")
		@RuneAttribute("booleanTypes")
		public List<Boolean> getBooleanTypes() {
			return booleanTypes;
		}
		
		@Override
		@RosettaAttribute("numberTypes")
		@RuneAttribute("numberTypes")
		public List<BigDecimal> getNumberTypes() {
			return numberTypes;
		}
		
		@Override
		@RosettaAttribute("parameterisedNumberTypes")
		@RuneAttribute("parameterisedNumberTypes")
		public List<BigDecimal> getParameterisedNumberTypes() {
			return parameterisedNumberTypes;
		}
		
		@Override
		@RosettaAttribute("stringTypes")
		@RuneAttribute("stringTypes")
		public List<String> getStringTypes() {
			return stringTypes;
		}
		
		@Override
		@RosettaAttribute("timeTypes")
		@RuneAttribute("timeTypes")
		public List<LocalTime> getTimeTypes() {
			return timeTypes;
		}
		
		@Override
		@RosettaAttribute("booleanTypes")
		@RuneAttribute("booleanTypes")
		public BasicListBuilder addBooleanTypes(Boolean _booleanTypes) {
			if (_booleanTypes != null) {
				this.booleanTypes.add(_booleanTypes);
			}
			return this;
		}
		
		@Override
		public BasicListBuilder addBooleanTypes(Boolean _booleanTypes, int _idx) {
			getIndex(this.booleanTypes, _idx, () -> _booleanTypes);
			return this;
		}
		
		@Override 
		public BasicListBuilder addBooleanTypes(List<Boolean> booleanTypess) {
			if (booleanTypess != null) {
				for (final Boolean toAdd : booleanTypess) {
					this.booleanTypes.add(toAdd);
				}
			}
			return this;
		}
		
		@Override
		@RuneAttribute("booleanTypes")
		public BasicListBuilder setBooleanTypes(List<Boolean> booleanTypess) {
			if (booleanTypess == null) {
				this.booleanTypes = new ArrayList<>();
			} else {
				this.booleanTypes = booleanTypess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		@RosettaAttribute("numberTypes")
		@RuneAttribute("numberTypes")
		public BasicListBuilder addNumberTypes(BigDecimal _numberTypes) {
			if (_numberTypes != null) {
				this.numberTypes.add(_numberTypes);
			}
			return this;
		}
		
		@Override
		public BasicListBuilder addNumberTypes(BigDecimal _numberTypes, int _idx) {
			getIndex(this.numberTypes, _idx, () -> _numberTypes);
			return this;
		}
		
		@Override
		public BasicListBuilder addNumberTypes(List<BigDecimal> numberTypess) {
			if (numberTypess != null) {
				for (final BigDecimal toAdd : numberTypess) {
					this.numberTypes.add(toAdd);
				}
			}
			return this;
		}
		
		@Override
		@RuneAttribute("numberTypes")
		public BasicListBuilder setNumberTypes(List<BigDecimal> numberTypess) {
			if (numberTypess == null) {
				this.numberTypes = new ArrayList<>();
			} else {
				this.numberTypes = numberTypess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		@RosettaAttribute("parameterisedNumberTypes")
		@RuneAttribute("parameterisedNumberTypes")
		public BasicListBuilder addParameterisedNumberTypes(BigDecimal _parameterisedNumberTypes) {
			if (_parameterisedNumberTypes != null) {
				this.parameterisedNumberTypes.add(_parameterisedNumberTypes);
			}
			return this;
		}
		
		@Override
		public BasicListBuilder addParameterisedNumberTypes(BigDecimal _parameterisedNumberTypes, int _idx) {
			getIndex(this.parameterisedNumberTypes, _idx, () -> _parameterisedNumberTypes);
			return this;
		}
		
		@Override 
		public BasicListBuilder addParameterisedNumberTypes(List<BigDecimal> parameterisedNumberTypess) {
			if (parameterisedNumberTypess != null) {
				for (final BigDecimal toAdd : parameterisedNumberTypess) {
					this.parameterisedNumberTypes.add(toAdd);
				}
			}
			return this;
		}
		
		@Override
		@RuneAttribute("parameterisedNumberTypes")
		public BasicListBuilder setParameterisedNumberTypes(List<BigDecimal> parameterisedNumberTypess) {
			if (parameterisedNumberTypess == null) {
				this.parameterisedNumberTypes = new ArrayList<>();
			} else {
				this.parameterisedNumberTypes = parameterisedNumberTypess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		@RosettaAttribute("stringTypes")
		@RuneAttribute("stringTypes")
		public BasicListBuilder addStringTypes(String _stringTypes) {
			if (_stringTypes != null) {
				this.stringTypes.add(_stringTypes);
			}
			return this;
		}
		
		@Override
		public BasicListBuilder addStringTypes(String _stringTypes, int _idx) {
			getIndex(this.stringTypes, _idx, () -> _stringTypes);
			return this;
		}
		
		@Override 
		public BasicListBuilder addStringTypes(List<String> stringTypess) {
			if (stringTypess != null) {
				for (final String toAdd : stringTypess) {
					this.stringTypes.add(toAdd);
				}
			}
			return this;
		}
		
		@Override
		@RuneAttribute("stringTypes")
		public BasicListBuilder setStringTypes(List<String> stringTypess) {
			if (stringTypess == null) {
				this.stringTypes = new ArrayList<>();
			} else {
				this.stringTypes = stringTypess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		@RosettaAttribute("timeTypes")
		@RuneAttribute("timeTypes")
		public BasicListBuilder addTimeTypes(LocalTime _timeTypes) {
			if (_timeTypes != null) {
				this.timeTypes.add(_timeTypes);
			}
			return this;
		}
		
		@Override
		public BasicListBuilder addTimeTypes(LocalTime _timeTypes, int _idx) {
			getIndex(this.timeTypes, _idx, () -> _timeTypes);
			return this;
		}

		@Override
		public BasicListBuilder addTimeTypes(List<LocalTime> timeTypess) {
			if (timeTypess != null) {
				for (final LocalTime toAdd : timeTypess) {
					this.timeTypes.add(toAdd);
				}
			}
			return this;
		}

		@Override
		@RuneAttribute("timeTypes")
		public BasicListBuilder setTimeTypes(List<LocalTime> timeTypess) {
			if (timeTypess == null) {
				this.timeTypes = new ArrayList<>();
			} else {
				this.timeTypes = timeTypess.stream()
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}

		@Override
		public BasicList build() {
			return new BasicListImpl(this);
		}
		
		@Override
		public BasicListBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public BasicListBuilder prune() {
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBooleanTypes()!=null && !getBooleanTypes().isEmpty()) return true;
			if (getNumberTypes()!=null && !getNumberTypes().isEmpty()) return true;
			if (getParameterisedNumberTypes()!=null && !getParameterisedNumberTypes().isEmpty()) return true;
			if (getStringTypes()!=null && !getStringTypes().isEmpty()) return true;
			if (getTimeTypes()!=null && !getTimeTypes().isEmpty()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public BasicListBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			BasicListBuilder o = (BasicListBuilder) other;
			
			
			merger.mergeBasic(getBooleanTypes(), o.getBooleanTypes(), (Consumer<Boolean>) this::addBooleanTypes);
			merger.mergeBasic(getNumberTypes(), o.getNumberTypes(), (Consumer<BigDecimal>) this::addNumberTypes);
			merger.mergeBasic(getParameterisedNumberTypes(), o.getParameterisedNumberTypes(), (Consumer<BigDecimal>) this::addParameterisedNumberTypes);
			merger.mergeBasic(getStringTypes(), o.getStringTypes(), (Consumer<String>) this::addStringTypes);
			merger.mergeBasic(getTimeTypes(), o.getTimeTypes(), (Consumer<LocalTime>) this::addTimeTypes);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			BasicList _that = getType().cast(o);
		
			if (!ListEquals.listEquals(booleanTypes, _that.getBooleanTypes())) return false;
			if (!ListEquals.listEquals(numberTypes, _that.getNumberTypes())) return false;
			if (!ListEquals.listEquals(parameterisedNumberTypes, _that.getParameterisedNumberTypes())) return false;
			if (!ListEquals.listEquals(stringTypes, _that.getStringTypes())) return false;
			if (!ListEquals.listEquals(timeTypes, _that.getTimeTypes())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (booleanTypes != null ? booleanTypes.hashCode() : 0);
			_result = 31 * _result + (numberTypes != null ? numberTypes.hashCode() : 0);
			_result = 31 * _result + (parameterisedNumberTypes != null ? parameterisedNumberTypes.hashCode() : 0);
			_result = 31 * _result + (stringTypes != null ? stringTypes.hashCode() : 0);
			_result = 31 * _result + (timeTypes != null ? timeTypes.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "BasicListBuilder {" +
				"booleanTypes=" + this.booleanTypes + ", " +
				"numberTypes=" + this.numberTypes + ", " +
				"parameterisedNumberTypes=" + this.parameterisedNumberTypes + ", " +
				"stringTypes=" + this.stringTypes + ", " +
				"timeTypes=" + this.timeTypes +
			'}';
		}
	}
}
