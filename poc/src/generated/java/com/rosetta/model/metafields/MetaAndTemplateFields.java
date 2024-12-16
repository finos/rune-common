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
import com.rosetta.model.lib.meta.TemplateFields;
import com.rosetta.model.lib.meta.TemplateFields.TemplateFieldsBuilder;
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
@RosettaDataType(value="MetaAndTemplateFields", builder=MetaAndTemplateFields.MetaAndTemplateFieldsBuilderImpl.class, version="0.0.0")
public interface MetaAndTemplateFields extends RosettaModelObject, GlobalKeyFields, TemplateFields, MetaDataFields {

	MetaAndTemplateFieldsMeta metaData = new MetaAndTemplateFieldsMeta();

	/*********************** Getter Methods  ***********************/
	String getScheme();
	String getTemplateGlobalReference();
	String getGlobalKey();
	String getExternalKey();
	List<? extends Key> getKey();

	/*********************** Build Methods  ***********************/
	MetaAndTemplateFields build();
	
	MetaAndTemplateFieldsBuilder toBuilder();
	
	static MetaAndTemplateFieldsBuilder builder() {
		return new MetaAndTemplateFieldsBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends MetaAndTemplateFields> metaData() {
		return metaData;
	}
	
	@Override
	default Class<? extends MetaAndTemplateFields> getType() {
		return MetaAndTemplateFields.class;
	}
	
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processor.processBasic(path.newSubPath("scheme"), String.class, getScheme(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("templateGlobalReference"), String.class, getTemplateGlobalReference(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("globalKey"), String.class, getGlobalKey(), this, AttributeMeta.META);
		processor.processBasic(path.newSubPath("externalKey"), String.class, getExternalKey(), this, AttributeMeta.META);
		processRosetta(path.newSubPath("key"), processor, Key.class, getKey());
	}
	

	/*********************** Builder Interface  ***********************/
	interface MetaAndTemplateFieldsBuilder extends MetaAndTemplateFields, RosettaModelObjectBuilder, GlobalKeyFieldsBuilder, TemplateFieldsBuilder, MetaDataFieldsBuilder {
		Key.KeyBuilder getOrCreateKey(int _index);
		List<? extends Key.KeyBuilder> getKey();
		MetaAndTemplateFieldsBuilder setScheme(String scheme);
		MetaAndTemplateFieldsBuilder setTemplateGlobalReference(String templateGlobalReference);
		MetaAndTemplateFieldsBuilder setGlobalKey(String globalKey);
		MetaAndTemplateFieldsBuilder setExternalKey(String externalKey);
		MetaAndTemplateFieldsBuilder addKey(Key key0);
		MetaAndTemplateFieldsBuilder addKey(Key key1, int _idx);
		MetaAndTemplateFieldsBuilder addKey(List<? extends Key> key2);
		MetaAndTemplateFieldsBuilder setKey(List<? extends Key> key3);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processor.processBasic(path.newSubPath("scheme"), String.class, getScheme(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("templateGlobalReference"), String.class, getTemplateGlobalReference(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("globalKey"), String.class, getGlobalKey(), this, AttributeMeta.META);
			processor.processBasic(path.newSubPath("externalKey"), String.class, getExternalKey(), this, AttributeMeta.META);
			processRosetta(path.newSubPath("key"), processor, Key.KeyBuilder.class, getKey());
		}
		

		MetaAndTemplateFieldsBuilder prune();
	}

	/*********************** Immutable Implementation of MetaAndTemplateFields  ***********************/
	class MetaAndTemplateFieldsImpl implements MetaAndTemplateFields {
		private final String scheme;
		private final String templateGlobalReference;
		private final String globalKey;
		private final String externalKey;
		private final List<? extends Key> key;
		
		protected MetaAndTemplateFieldsImpl(MetaAndTemplateFieldsBuilder builder) {
			this.scheme = builder.getScheme();
			this.templateGlobalReference = builder.getTemplateGlobalReference();
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
		@RosettaAttribute("templateGlobalReference")
		public String getTemplateGlobalReference() {
			return templateGlobalReference;
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
		public MetaAndTemplateFields build() {
			return this;
		}
		
		@Override
		public MetaAndTemplateFieldsBuilder toBuilder() {
			MetaAndTemplateFieldsBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(MetaAndTemplateFieldsBuilder builder) {
			ofNullable(getScheme()).ifPresent(builder::setScheme);
			ofNullable(getTemplateGlobalReference()).ifPresent(builder::setTemplateGlobalReference);
			ofNullable(getGlobalKey()).ifPresent(builder::setGlobalKey);
			ofNullable(getExternalKey()).ifPresent(builder::setExternalKey);
			ofNullable(getKey()).ifPresent(builder::setKey);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			MetaAndTemplateFields _that = getType().cast(o);
		
			if (!Objects.equals(scheme, _that.getScheme())) return false;
			if (!Objects.equals(templateGlobalReference, _that.getTemplateGlobalReference())) return false;
			if (!Objects.equals(globalKey, _that.getGlobalKey())) return false;
			if (!Objects.equals(externalKey, _that.getExternalKey())) return false;
			if (!ListEquals.listEquals(key, _that.getKey())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (scheme != null ? scheme.hashCode() : 0);
			_result = 31 * _result + (templateGlobalReference != null ? templateGlobalReference.hashCode() : 0);
			_result = 31 * _result + (globalKey != null ? globalKey.hashCode() : 0);
			_result = 31 * _result + (externalKey != null ? externalKey.hashCode() : 0);
			_result = 31 * _result + (key != null ? key.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "MetaAndTemplateFields {" +
				"scheme=" + this.scheme + ", " +
				"templateGlobalReference=" + this.templateGlobalReference + ", " +
				"globalKey=" + this.globalKey + ", " +
				"externalKey=" + this.externalKey + ", " +
				"key=" + this.key +
			'}';
		}
	}

	/*********************** Builder Implementation of MetaAndTemplateFields  ***********************/
	class MetaAndTemplateFieldsBuilderImpl implements MetaAndTemplateFieldsBuilder {
	
		protected String scheme;
		protected String templateGlobalReference;
		protected String globalKey;
		protected String externalKey;
		protected List<Key.KeyBuilder> key = new ArrayList<>();
	
		public MetaAndTemplateFieldsBuilderImpl() {
		}
	
		@Override
		@RosettaAttribute("scheme")
		public String getScheme() {
			return scheme;
		}
		
		@Override
		@RosettaAttribute("templateGlobalReference")
		public String getTemplateGlobalReference() {
			return templateGlobalReference;
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
		public MetaAndTemplateFieldsBuilder setScheme(String scheme) {
			this.scheme = scheme==null?null:scheme;
			return this;
		}
		@Override
		@RosettaAttribute("templateGlobalReference")
		public MetaAndTemplateFieldsBuilder setTemplateGlobalReference(String templateGlobalReference) {
			this.templateGlobalReference = templateGlobalReference==null?null:templateGlobalReference;
			return this;
		}
		@Override
		@RosettaAttribute("globalKey")
		public MetaAndTemplateFieldsBuilder setGlobalKey(String globalKey) {
			this.globalKey = globalKey==null?null:globalKey;
			return this;
		}
		@Override
		@RosettaAttribute("externalKey")
		public MetaAndTemplateFieldsBuilder setExternalKey(String externalKey) {
			this.externalKey = externalKey==null?null:externalKey;
			return this;
		}
		@Override
		@RosettaAttribute("location")
		public MetaAndTemplateFieldsBuilder addKey(Key key) {
			if (key!=null) this.key.add(key.toBuilder());
			return this;
		}
		
		@Override
		public MetaAndTemplateFieldsBuilder addKey(Key key, int _idx) {
			getIndex(this.key, _idx, () -> key.toBuilder());
			return this;
		}
		@Override 
		public MetaAndTemplateFieldsBuilder addKey(List<? extends Key> keys) {
			if (keys != null) {
				for (Key toAdd : keys) {
					this.key.add(toAdd.toBuilder());
				}
			}
			return this;
		}
		
		@Override 
		public MetaAndTemplateFieldsBuilder setKey(List<? extends Key> keys) {
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
		public MetaAndTemplateFields build() {
			return new MetaAndTemplateFieldsImpl(this);
		}
		
		@Override
		public MetaAndTemplateFieldsBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public MetaAndTemplateFieldsBuilder prune() {
			key = key.stream().filter(b->b!=null).<Key.KeyBuilder>map(b->b.prune()).filter(b->b.hasData()).collect(Collectors.toList());
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getScheme()!=null) return true;
			if (getTemplateGlobalReference()!=null) return true;
			if (getGlobalKey()!=null) return true;
			if (getExternalKey()!=null) return true;
			if (getKey()!=null && getKey().stream().filter(Objects::nonNull).anyMatch(a->a.hasData())) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public MetaAndTemplateFieldsBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			MetaAndTemplateFieldsBuilder o = (MetaAndTemplateFieldsBuilder) other;
			
			merger.mergeRosetta(getKey(), o.getKey(), this::getOrCreateKey);
			
			merger.mergeBasic(getScheme(), o.getScheme(), this::setScheme);
			merger.mergeBasic(getTemplateGlobalReference(), o.getTemplateGlobalReference(), this::setTemplateGlobalReference);
			merger.mergeBasic(getGlobalKey(), o.getGlobalKey(), this::setGlobalKey);
			merger.mergeBasic(getExternalKey(), o.getExternalKey(), this::setExternalKey);
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			MetaAndTemplateFields _that = getType().cast(o);
		
			if (!Objects.equals(scheme, _that.getScheme())) return false;
			if (!Objects.equals(templateGlobalReference, _that.getTemplateGlobalReference())) return false;
			if (!Objects.equals(globalKey, _that.getGlobalKey())) return false;
			if (!Objects.equals(externalKey, _that.getExternalKey())) return false;
			if (!ListEquals.listEquals(key, _that.getKey())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (scheme != null ? scheme.hashCode() : 0);
			_result = 31 * _result + (templateGlobalReference != null ? templateGlobalReference.hashCode() : 0);
			_result = 31 * _result + (globalKey != null ? globalKey.hashCode() : 0);
			_result = 31 * _result + (externalKey != null ? externalKey.hashCode() : 0);
			_result = 31 * _result + (key != null ? key.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "MetaAndTemplateFieldsBuilder {" +
				"scheme=" + this.scheme + ", " +
				"templateGlobalReference=" + this.templateGlobalReference + ", " +
				"globalKey=" + this.globalKey + ", " +
				"externalKey=" + this.externalKey + ", " +
				"key=" + this.key +
			'}';
		}
	}
}

class MetaAndTemplateFieldsMeta extends BasicRosettaMetaData<MetaAndTemplateFields>{

}
