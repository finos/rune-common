package test.metascheme;

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
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import test.metascheme.Root.RootBuilderImpl;
import test.metascheme.meta.RootMeta;
import test.metascheme.metafields.FieldWithMetaA;
import test.metascheme.metafields.FieldWithMetaA.FieldWithMetaABuilder;
import test.metascheme.metafields.FieldWithMetaEnumType;
import test.metascheme.metafields.FieldWithMetaEnumType.FieldWithMetaEnumTypeBuilder;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Root", builder= RootBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Root", model="Just another Rosetta model", builder= RootBuilderImpl.class, version="0.0.0")
public interface Root extends RosettaModelObject {

	RootMeta metaData = new RootMeta();

	/*********************** Getter Methods  ***********************/
	FieldWithMetaEnumType getEnumType();
	FieldWithMetaA getTypeA();
	List<? extends FieldWithMetaEnumType> getEnumTypeList();
	List<? extends FieldWithMetaA> getTypeAList();

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
		processRosetta(path.newSubPath("enumType"), processor, FieldWithMetaEnumType.class, getEnumType());
		processRosetta(path.newSubPath("typeA"), processor, FieldWithMetaA.class, getTypeA());
		processRosetta(path.newSubPath("enumTypeList"), processor, FieldWithMetaEnumType.class, getEnumTypeList());
		processRosetta(path.newSubPath("typeAList"), processor, FieldWithMetaA.class, getTypeAList());
	}
	

	/*********************** Builder Interface  ***********************/
	interface RootBuilder extends Root, RosettaModelObjectBuilder {
		FieldWithMetaEnumTypeBuilder getOrCreateEnumType();
		@Override
		FieldWithMetaEnumTypeBuilder getEnumType();
		FieldWithMetaABuilder getOrCreateTypeA();
		@Override
		FieldWithMetaABuilder getTypeA();
		FieldWithMetaEnumTypeBuilder getOrCreateEnumTypeList(int _index);
		@Override
		List<? extends FieldWithMetaEnumTypeBuilder> getEnumTypeList();
		FieldWithMetaABuilder getOrCreateTypeAList(int _index);
		@Override
		List<? extends FieldWithMetaABuilder> getTypeAList();
		RootBuilder setEnumType(FieldWithMetaEnumType enumType);
		RootBuilder setEnumTypeValue(EnumType enumType);
		RootBuilder setTypeA(FieldWithMetaA typeA);
		RootBuilder setTypeAValue(A typeA);
		RootBuilder addEnumTypeList(FieldWithMetaEnumType enumTypeList);
		RootBuilder addEnumTypeList(FieldWithMetaEnumType enumTypeList, int _idx);
		RootBuilder addEnumTypeListValue(EnumType enumTypeList);
		RootBuilder addEnumTypeListValue(EnumType enumTypeList, int _idx);
		RootBuilder addEnumTypeList(List<? extends FieldWithMetaEnumType> enumTypeList);
		RootBuilder setEnumTypeList(List<? extends FieldWithMetaEnumType> enumTypeList);
		RootBuilder addEnumTypeListValue(List<? extends EnumType> enumTypeList);
		RootBuilder setEnumTypeListValue(List<? extends EnumType> enumTypeList);
		RootBuilder addTypeAList(FieldWithMetaA typeAList);
		RootBuilder addTypeAList(FieldWithMetaA typeAList, int _idx);
		RootBuilder addTypeAListValue(A typeAList);
		RootBuilder addTypeAListValue(A typeAList, int _idx);
		RootBuilder addTypeAList(List<? extends FieldWithMetaA> typeAList);
		RootBuilder setTypeAList(List<? extends FieldWithMetaA> typeAList);
		RootBuilder addTypeAListValue(List<? extends A> typeAList);
		RootBuilder setTypeAListValue(List<? extends A> typeAList);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("enumType"), processor, FieldWithMetaEnumTypeBuilder.class, getEnumType());
			processRosetta(path.newSubPath("typeA"), processor, FieldWithMetaABuilder.class, getTypeA());
			processRosetta(path.newSubPath("enumTypeList"), processor, FieldWithMetaEnumTypeBuilder.class, getEnumTypeList());
			processRosetta(path.newSubPath("typeAList"), processor, FieldWithMetaABuilder.class, getTypeAList());
		}
		

		RootBuilder prune();
	}

	/*********************** Immutable Implementation of Root  ***********************/
	class RootImpl implements Root {
		private final FieldWithMetaEnumType enumType;
		private final FieldWithMetaA typeA;
		private final List<? extends FieldWithMetaEnumType> enumTypeList;
		private final List<? extends FieldWithMetaA> typeAList;
		
		protected RootImpl(RootBuilder builder) {
			this.enumType = ofNullable(builder.getEnumType()).map(f->f.build()).orElse(null);
			this.typeA = ofNullable(builder.getTypeA()).map(f->f.build()).orElse(null);
			this.enumTypeList = ofNullable(builder.getEnumTypeList()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
			this.typeAList = ofNullable(builder.getTypeAList()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("enumType")
		@RuneAttribute("enumType")
		public FieldWithMetaEnumType getEnumType() {
			return enumType;
		}
		
		@Override
		@RosettaAttribute("typeA")
		@RuneAttribute("typeA")
		public FieldWithMetaA getTypeA() {
			return typeA;
		}
		
		@Override
		@RosettaAttribute("enumTypeList")
		@RuneAttribute("enumTypeList")
		public List<? extends FieldWithMetaEnumType> getEnumTypeList() {
			return enumTypeList;
		}
		
		@Override
		@RosettaAttribute("typeAList")
		@RuneAttribute("typeAList")
		public List<? extends FieldWithMetaA> getTypeAList() {
			return typeAList;
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
			ofNullable(getEnumType()).ifPresent(builder::setEnumType);
			ofNullable(getTypeA()).ifPresent(builder::setTypeA);
			ofNullable(getEnumTypeList()).ifPresent(builder::setEnumTypeList);
			ofNullable(getTypeAList()).ifPresent(builder::setTypeAList);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(enumType, _that.getEnumType())) return false;
			if (!Objects.equals(typeA, _that.getTypeA())) return false;
			if (!ListEquals.listEquals(enumTypeList, _that.getEnumTypeList())) return false;
			if (!ListEquals.listEquals(typeAList, _that.getTypeAList())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (enumType != null ? enumType.hashCode() : 0);
			_result = 31 * _result + (typeA != null ? typeA.hashCode() : 0);
			_result = 31 * _result + (enumTypeList != null ? enumTypeList.hashCode() : 0);
			_result = 31 * _result + (typeAList != null ? typeAList.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Root {" +
				"enumType=" + this.enumType + ", " +
				"typeA=" + this.typeA + ", " +
				"enumTypeList=" + this.enumTypeList + ", " +
				"typeAList=" + this.typeAList +
			'}';
		}
	}

	/*********************** Builder Implementation of Root  ***********************/
	class RootBuilderImpl implements RootBuilder {
	
		protected FieldWithMetaEnumTypeBuilder enumType;
		protected FieldWithMetaABuilder typeA;
		protected List<FieldWithMetaEnumTypeBuilder> enumTypeList = new ArrayList<>();
		protected List<FieldWithMetaABuilder> typeAList = new ArrayList<>();
		
		@Override
		@RosettaAttribute("enumType")
		@RuneAttribute("enumType")
		public FieldWithMetaEnumTypeBuilder getEnumType() {
			return enumType;
		}
		
		@Override
		public FieldWithMetaEnumTypeBuilder getOrCreateEnumType() {
			FieldWithMetaEnumTypeBuilder result;
			if (enumType!=null) {
				result = enumType;
			}
			else {
				result = enumType = FieldWithMetaEnumType.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("typeA")
		@RuneAttribute("typeA")
		public FieldWithMetaABuilder getTypeA() {
			return typeA;
		}
		
		@Override
		public FieldWithMetaABuilder getOrCreateTypeA() {
			FieldWithMetaABuilder result;
			if (typeA!=null) {
				result = typeA;
			}
			else {
				result = typeA = FieldWithMetaA.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("enumTypeList")
		@RuneAttribute("enumTypeList")
		public List<? extends FieldWithMetaEnumTypeBuilder> getEnumTypeList() {
			return enumTypeList;
		}
		
		@Override
		public FieldWithMetaEnumTypeBuilder getOrCreateEnumTypeList(int _index) {
		
			if (enumTypeList==null) {
				this.enumTypeList = new ArrayList<>();
			}
			FieldWithMetaEnumTypeBuilder result;
			return getIndex(enumTypeList, _index, () -> {
						FieldWithMetaEnumTypeBuilder newEnumTypeList = FieldWithMetaEnumType.builder();
						return newEnumTypeList;
					});
		}
		
		@Override
		@RosettaAttribute("typeAList")
		@RuneAttribute("typeAList")
		public List<? extends FieldWithMetaABuilder> getTypeAList() {
			return typeAList;
		}
		
		@Override
		public FieldWithMetaABuilder getOrCreateTypeAList(int _index) {
		
			if (typeAList==null) {
				this.typeAList = new ArrayList<>();
			}
			FieldWithMetaABuilder result;
			return getIndex(typeAList, _index, () -> {
						FieldWithMetaABuilder newTypeAList = FieldWithMetaA.builder();
						return newTypeAList;
					});
		}
		
		@Override
		@RosettaAttribute("enumType")
		@RuneAttribute("enumType")
		public RootBuilder setEnumType(FieldWithMetaEnumType _enumType) {
			this.enumType = _enumType == null ? null : _enumType.toBuilder();
			return this;
		}
		
		@Override
		public RootBuilder setEnumTypeValue(EnumType _enumType) {
			this.getOrCreateEnumType().setValue(_enumType);
			return this;
		}
		
		@Override
		@RosettaAttribute("typeA")
		@RuneAttribute("typeA")
		public RootBuilder setTypeA(FieldWithMetaA _typeA) {
			this.typeA = _typeA == null ? null : _typeA.toBuilder();
			return this;
		}
		
		@Override
		public RootBuilder setTypeAValue(A _typeA) {
			this.getOrCreateTypeA().setValue(_typeA);
			return this;
		}
		
		@Override
		@RosettaAttribute("enumTypeList")
		@RuneAttribute("enumTypeList")
		public RootBuilder addEnumTypeList(FieldWithMetaEnumType _enumTypeList) {
			if (_enumTypeList != null) {
				this.enumTypeList.add(_enumTypeList.toBuilder());
			}
			return this;
		}
		
		@Override
		public RootBuilder addEnumTypeList(FieldWithMetaEnumType _enumTypeList, int _idx) {
			getIndex(this.enumTypeList, _idx, () -> _enumTypeList.toBuilder());
			return this;
		}
		
		@Override
		public RootBuilder addEnumTypeListValue(EnumType _enumTypeList) {
			this.getOrCreateEnumTypeList(-1).setValue(_enumTypeList);
			return this;
		}
		
		@Override
		public RootBuilder addEnumTypeListValue(EnumType _enumTypeList, int _idx) {
			this.getOrCreateEnumTypeList(_idx).setValue(_enumTypeList);
			return this;
		}
		
		@Override 
		public RootBuilder addEnumTypeList(List<? extends FieldWithMetaEnumType> enumTypeLists) {
			if (enumTypeLists != null) {
				for (final FieldWithMetaEnumType toAdd : enumTypeLists) {
					this.enumTypeList.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@Override 
		@RuneAttribute("enumTypeList")
		public RootBuilder setEnumTypeList(List<? extends FieldWithMetaEnumType> enumTypeLists) {
			if (enumTypeLists == null) {
				this.enumTypeList = new ArrayList<>();
			} else {
				this.enumTypeList = enumTypeLists.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public RootBuilder addEnumTypeListValue(List<? extends EnumType> enumTypeLists) {
			if (enumTypeLists != null) {
				for (final EnumType toAdd : enumTypeLists) {
					this.addEnumTypeListValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public RootBuilder setEnumTypeListValue(List<? extends EnumType> enumTypeLists) {
			this.enumTypeList.clear();
			if (enumTypeLists != null) {
				enumTypeLists.forEach(this::addEnumTypeListValue);
			}
			return this;
		}
		
		@Override
		@RosettaAttribute("typeAList")
		@RuneAttribute("typeAList")
		public RootBuilder addTypeAList(FieldWithMetaA _typeAList) {
			if (_typeAList != null) {
				this.typeAList.add(_typeAList.toBuilder());
			}
			return this;
		}
		
		@Override
		public RootBuilder addTypeAList(FieldWithMetaA _typeAList, int _idx) {
			getIndex(this.typeAList, _idx, () -> _typeAList.toBuilder());
			return this;
		}
		
		@Override
		public RootBuilder addTypeAListValue(A _typeAList) {
			this.getOrCreateTypeAList(-1).setValue(_typeAList.toBuilder());
			return this;
		}
		
		@Override
		public RootBuilder addTypeAListValue(A _typeAList, int _idx) {
			this.getOrCreateTypeAList(_idx).setValue(_typeAList.toBuilder());
			return this;
		}
		
		@Override 
		public RootBuilder addTypeAList(List<? extends FieldWithMetaA> typeALists) {
			if (typeALists != null) {
				for (final FieldWithMetaA toAdd : typeALists) {
					this.typeAList.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@Override 
		@RuneAttribute("typeAList")
		public RootBuilder setTypeAList(List<? extends FieldWithMetaA> typeALists) {
			if (typeALists == null) {
				this.typeAList = new ArrayList<>();
			} else {
				this.typeAList = typeALists.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		@Override
		public RootBuilder addTypeAListValue(List<? extends A> typeALists) {
			if (typeALists != null) {
				for (final A toAdd : typeALists) {
					this.addTypeAListValue(toAdd);
				}
			}
			return this;
		}
		
		@Override
		public RootBuilder setTypeAListValue(List<? extends A> typeALists) {
			this.typeAList.clear();
			if (typeALists != null) {
				typeALists.forEach(this::addTypeAListValue);
			}
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
			if (enumType!=null && !enumType.prune().hasData()) enumType = null;
			if (typeA!=null && !typeA.prune().hasData()) typeA = null;
			enumTypeList = enumTypeList.stream().filter(b->b!=null).<FieldWithMetaEnumTypeBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			typeAList = typeAList.stream().filter(b->b!=null).<FieldWithMetaABuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getEnumType()!=null) return true;
			if (getTypeA()!=null && getTypeA().hasData()) return true;
			if (getEnumTypeList()!=null && !getEnumTypeList().isEmpty()) return true;
			if (getTypeAList()!=null && getTypeAList().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RootBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RootBuilder o = (RootBuilder) other;
			
			merger.mergeRosetta(getEnumType(), o.getEnumType(), this::setEnumType);
			merger.mergeRosetta(getTypeA(), o.getTypeA(), this::setTypeA);
			merger.mergeRosetta(getEnumTypeList(), o.getEnumTypeList(), this::getOrCreateEnumTypeList);
			merger.mergeRosetta(getTypeAList(), o.getTypeAList(), this::getOrCreateTypeAList);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(enumType, _that.getEnumType())) return false;
			if (!Objects.equals(typeA, _that.getTypeA())) return false;
			if (!ListEquals.listEquals(enumTypeList, _that.getEnumTypeList())) return false;
			if (!ListEquals.listEquals(typeAList, _that.getTypeAList())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (enumType != null ? enumType.hashCode() : 0);
			_result = 31 * _result + (typeA != null ? typeA.hashCode() : 0);
			_result = 31 * _result + (enumTypeList != null ? enumTypeList.hashCode() : 0);
			_result = 31 * _result + (typeAList != null ? typeAList.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RootBuilder {" +
				"enumType=" + this.enumType + ", " +
				"typeA=" + this.typeA + ", " +
				"enumTypeList=" + this.enumTypeList + ", " +
				"typeAList=" + this.typeAList +
			'}';
		}
	}
}
