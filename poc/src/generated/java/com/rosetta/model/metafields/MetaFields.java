package com.rosetta.model.metafields;

import com.google.common.collect.ImmutableList;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.annotations.RosettaAttribute;
import com.rosetta.model.lib.annotations.RosettaDataType;
import com.rosetta.model.lib.meta.BasicRosettaMetaData;
import com.rosetta.model.lib.meta.GlobalKeyFields;
import com.rosetta.model.lib.meta.GlobalKeyFields.GlobalKeyFieldsBuilder;
import com.rosetta.model.lib.meta.Key;
import com.rosetta.model.lib.meta.MetaDataFields;
import com.rosetta.model.lib.meta.MetaDataFields.MetaDataFieldsBuilder;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.AttributeMeta;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;
import com.rosetta.util.ListEquals;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

/**
 * @version 1
 */
@RosettaDataType(value="MetaFields", builder=MetaFields.MetaFieldsBuilderImpl.class, version="0.0.0")
public interface MetaFields extends RosettaModelObject, GlobalKeyFields, MetaDataFields {

	MetaFieldsMeta metaData = new MetaFieldsMeta();

	/*********************** Getter Methods  ***********************/
	String getScheme();
	String getGlobalKey();
	String getExternalKey();
	List<? extends Key> getKey();

	/*********************** Build Methods  ***********************/
	MetaFields build();
	
	MetaFieldsBuilder toBuilder();
	
	static MetaFieldsBuilder builder() {
		return new MetaFieldsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends MetaFields> metaData() {
		return metaData;
	}
	
	@Override
	default Class<? extends MetaFields> getType() {
		return MetaFields.class;
	}
	
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("scheme"), String.class, getScheme(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("globalKey"), String.class, getGlobalKey(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("externalKey"), String.class, getExternalKey(), this, AttributeMeta.META);
		processRosetta(path.newSubPath("key"), processor, Key.class, getKey());
	}
	

	/*********************** Builder Interface  ***********************/
	interface MetaFieldsBuilder extends MetaFields, RosettaModelObjectBuilder, GlobalKeyFieldsBuilder, MetaDataFieldsBuilder {
		Key.KeyBuilder getOrCreateKey(int _index);
		List<? extends Key.KeyBuilder> getKey();
		MetaFieldsBuilder setScheme(String scheme);
		MetaFieldsBuilder setGlobalKey(String globalKey);
		MetaFieldsBuilder setExternalKey(String externalKey);
		MetaFieldsBuilder addKey(Key key0);
		MetaFieldsBuilder addKey(Key key1, int _idx);
		MetaFieldsBuilder addKey(List<? extends Key> key2);
		MetaFieldsBuilder setKey(List<? extends Key> key3);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("scheme"), String.class, getScheme(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("globalKey"), String.class, getGlobalKey(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("externalKey"), String.class, getExternalKey(), this, AttributeMeta.META);
			processRosetta(path.newSubPath("key"), processor, Key.KeyBuilder.class, getKey());
		}
		

		MetaFieldsBuilder prune();
	}

	/*********************** Immutable Implementation of MetaFields  ***********************/
	class MetaFieldsImpl implements MetaFields {
		private final String scheme;
		private final String globalKey;
		private final String externalKey;
		private final List<? extends Key> key;
		
		protected MetaFieldsImpl(MetaFieldsBuilder builder) {
			this.scheme = builder.getScheme();
			this.globalKey = builder.getGlobalKey();
			this.externalKey = builder.getExternalKey();
			this.key = ofNullable(builder.getKey()).filter(_l->!_l.isEmpty()).map(list -> list.stream().filter(Objects::nonNull).map(f->f.build()).filter(Objects::nonNull).collect(ImmutableList.toImmutableList())).orElse(null);
		}
		
		@Override
		@RosettaAttribute("scheme")
		public String getScheme() {
			return scheme;
		}
		
		@Override
		@RosettaAttribute("globalKey")
		public String getGlobalKey() {
			return globalKey;
		}
		
		@Override
		@RosettaAttribute("externalKey")
		public String getExternalKey() {
			return externalKey;
		}
		
		@Override
		@RosettaAttribute("location")
		public List<? extends Key> getKey() {
			return key;
		}
		
		@Override
		public MetaFields build() {
			return this;
		}
		
		@Override
		public MetaFieldsBuilder toBuilder() {
			MetaFieldsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(MetaFieldsBuilder builder) {
			ofNullable(getScheme()).ifPresent(builder::setScheme);
			ofNullable(getGlobalKey()).ifPresent(builder::setGlobalKey);
			ofNullable(getExternalKey()).ifPresent(builder::setExternalKey);
			ofNullable(getKey()).ifPresent(builder::setKey);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			MetaFields _that = getType().cast(o);
		
			if (!Objects.equals(scheme, _that.getScheme())) return false;
			if (!Objects.equals(globalKey, _that.getGlobalKey())) return false;
			if (!Objects.equals(externalKey, _that.getExternalKey())) return false;
			if (!ListEquals.listEquals(key, _that.getKey())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (scheme != null ? scheme.hashCode() : 0);
			_result = 31 * _result + (globalKey != null ? globalKey.hashCode() : 0);
			_result = 31 * _result + (externalKey != null ? externalKey.hashCode() : 0);
			_result = 31 * _result + (key != null ? key.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "MetaFields {" +
				"scheme=" + this.scheme + ", " +
				"globalKey=" + this.globalKey + ", " +
				"externalKey=" + this.externalKey + ", " +
				"key=" + this.key +
			'}';
		}
	}

	/*********************** Builder Implementation of MetaFields  ***********************/
	class MetaFieldsBuilderImpl implements MetaFieldsBuilder {
	
		protected String scheme;
		protected String globalKey;
		protected String externalKey;
		protected List<Key.KeyBuilder> key = new ArrayList<>();
	
		public MetaFieldsBuilderImpl() {
		}
	
		@Override
		@RosettaAttribute("scheme")
		public String getScheme() {
			return scheme;
		}
		
		@Override
		@RosettaAttribute("globalKey")
		public String getGlobalKey() {
			return globalKey;
		}
		
		@Override
		@RosettaAttribute("externalKey")
		public String getExternalKey() {
			return externalKey;
		}
		
		@Override
		@RosettaAttribute("location")
		public List<? extends Key.KeyBuilder> getKey() {
			return key;
		}
		
		@Override
		public Key.KeyBuilder getOrCreateKey(int _index) {
		
			if (key==null) {
				this.key = new ArrayList<>();
			}
			Key.KeyBuilder result;
			return getIndex(key, _index, () -> {
						Key.KeyBuilder newKey = Key.builder();
						return newKey;
					});
		}
		
		@Override
		@RosettaAttribute("scheme")
		public MetaFieldsBuilder setScheme(String scheme) {
			this.scheme = scheme==null?null:scheme;
			return this;
		}
		@Override
		@RosettaAttribute("globalKey")
		public MetaFieldsBuilder setGlobalKey(String globalKey) {
			this.globalKey = globalKey==null?null:globalKey;
			return this;
		}
		@Override
		@RosettaAttribute("externalKey")
		public MetaFieldsBuilder setExternalKey(String externalKey) {
			this.externalKey = externalKey==null?null:externalKey;
			return this;
		}
		@Override
		@RosettaAttribute("location")
		public MetaFieldsBuilder addKey(Key key) {
			if (key!=null) this.key.add(key.toBuilder());
			return this;
		}
		
		@Override
		public MetaFieldsBuilder addKey(Key key, int _idx) {
			getIndex(this.key, _idx, () -> key.toBuilder());
			return this;
		}
		@Override 
		public MetaFieldsBuilder addKey(List<? extends Key> keys) {
			if (keys != null) {
				for (Key toAdd : keys) {
					this.key.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@Override 
		public MetaFieldsBuilder setKey(List<? extends Key> keys) {
			if (keys == null)  {
				this.key = new ArrayList<>();
			}
			else {
				this.key = keys.stream()
					.map(_a->_a.toBuilder())
					.collect(Collectors.toCollection(()->new ArrayList<>()));
			}
			return this;
		}
		
		
		@Override
		public MetaFields build() {
			return new MetaFieldsImpl(this);
		}
		
		@Override
		public MetaFieldsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public MetaFieldsBuilder prune() {
			key = key.stream().filter(b->b!=null).<Key.KeyBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getScheme()!=null) return true;
			if (getGlobalKey()!=null) return true;
			if (getExternalKey()!=null) return true;
			if (getKey()!=null && getKey().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public MetaFieldsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			MetaFieldsBuilder o = (MetaFieldsBuilder) other;
			
			merger.mergeRosetta(getKey(), o.getKey(), this::getOrCreateKey);
			
			merger.mergeBasic(getScheme(), o.getScheme(), this::setScheme);
			merger.mergeBasic(getGlobalKey(), o.getGlobalKey(), this::setGlobalKey);
			merger.mergeBasic(getExternalKey(), o.getExternalKey(), this::setExternalKey);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			MetaFields _that = getType().cast(o);
		
			if (!Objects.equals(scheme, _that.getScheme())) return false;
			if (!Objects.equals(globalKey, _that.getGlobalKey())) return false;
			if (!Objects.equals(externalKey, _that.getExternalKey())) return false;
			if (!ListEquals.listEquals(key, _that.getKey())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (scheme != null ? scheme.hashCode() : 0);
			_result = 31 * _result + (globalKey != null ? globalKey.hashCode() : 0);
			_result = 31 * _result + (externalKey != null ? externalKey.hashCode() : 0);
			_result = 31 * _result + (key != null ? key.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "MetaFieldsBuilder {" +
				"scheme=" + this.scheme + ", " +
				"globalKey=" + this.globalKey + ", " +
				"externalKey=" + this.externalKey + ", " +
				"key=" + this.key +
			'}';
		}
	}
}

class MetaFieldsMeta extends BasicRosettaMetaData<MetaFields>{

}
