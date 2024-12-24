package test.basic;

import test.basic.BasicList.BasicListBuilder;
import test.basic.BasicSingle.BasicSingleBuilder;
import test.basic.Root.RootBuilderImpl;
import test.basic.meta.RootMeta;
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

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Root", builder= RootBuilderImpl.class, version="0.0.0")
@RuneDataType(value="Root", model="test", builder= RootBuilderImpl.class, version="0.0.0")
public interface Root extends RosettaModelObject {

	RootMeta metaData = new RootMeta();

	/*********************** Getter Methods  ***********************/
	BasicSingle getBasicSingle();
	BasicList getBasicList();

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
		processRosetta(path.newSubPath("basicSingle"), processor, BasicSingle.class, getBasicSingle());
		processRosetta(path.newSubPath("basicList"), processor, BasicList.class, getBasicList());
	}
	

	/*********************** Builder Interface  ***********************/
	interface RootBuilder extends Root, RosettaModelObjectBuilder {
		BasicSingleBuilder getOrCreateBasicSingle();
		@Override
		BasicSingleBuilder getBasicSingle();
		BasicListBuilder getOrCreateBasicList();
		@Override
		BasicListBuilder getBasicList();
		RootBuilder setBasicSingle(BasicSingle basicSingle);
		RootBuilder setBasicList(BasicList basicList);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("basicSingle"), processor, BasicSingleBuilder.class, getBasicSingle());
			processRosetta(path.newSubPath("basicList"), processor, BasicListBuilder.class, getBasicList());
		}
		

		RootBuilder prune();
	}

	/*********************** Immutable Implementation of Root  ***********************/
	class RootImpl implements Root {
		private final BasicSingle basicSingle;
		private final BasicList basicList;
		
		protected RootImpl(RootBuilder builder) {
			this.basicSingle = ofNullable(builder.getBasicSingle()).map(f->f.build()).orElse(null);
			this.basicList = ofNullable(builder.getBasicList()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("basicSingle")
		@RuneAttribute("basicSingle")
		public BasicSingle getBasicSingle() {
			return basicSingle;
		}
		
		@Override
		@RosettaAttribute("basicList")
		@RuneAttribute("basicList")
		public BasicList getBasicList() {
			return basicList;
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
			ofNullable(getBasicSingle()).ifPresent(builder::setBasicSingle);
			ofNullable(getBasicList()).ifPresent(builder::setBasicList);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(basicSingle, _that.getBasicSingle())) return false;
			if (!Objects.equals(basicList, _that.getBasicList())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (basicSingle != null ? basicSingle.hashCode() : 0);
			_result = 31 * _result + (basicList != null ? basicList.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Root {" +
				"basicSingle=" + this.basicSingle + ", " +
				"basicList=" + this.basicList +
			'}';
		}
	}

	/*********************** Builder Implementation of Root  ***********************/
	class RootBuilderImpl implements RootBuilder {
	
		protected BasicSingleBuilder basicSingle;
		protected BasicListBuilder basicList;
		
		@Override
		@RosettaAttribute("basicSingle")
		@RuneAttribute("basicSingle")
		public BasicSingleBuilder getBasicSingle() {
			return basicSingle;
		}
		
		@Override
		public BasicSingleBuilder getOrCreateBasicSingle() {
			BasicSingleBuilder result;
			if (basicSingle!=null) {
				result = basicSingle;
			}
			else {
				result = basicSingle = BasicSingle.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("basicList")
		@RuneAttribute("basicList")
		public BasicListBuilder getBasicList() {
			return basicList;
		}
		
		@Override
		public BasicListBuilder getOrCreateBasicList() {
			BasicListBuilder result;
			if (basicList!=null) {
				result = basicList;
			}
			else {
				result = basicList = BasicList.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("basicSingle")
		@RuneAttribute("basicSingle")
		public RootBuilder setBasicSingle(BasicSingle _basicSingle) {
			this.basicSingle = _basicSingle == null ? null : _basicSingle.toBuilder();
			return this;
		}
		
		@Override
		@RosettaAttribute("basicList")
		@RuneAttribute("basicList")
		public RootBuilder setBasicList(BasicList _basicList) {
			this.basicList = _basicList == null ? null : _basicList.toBuilder();
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
			if (basicSingle!=null && !basicSingle.prune().hasData()) basicSingle = null;
			if (basicList!=null && !basicList.prune().hasData()) basicList = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getBasicSingle()!=null && getBasicSingle().hasData()) return true;
			if (getBasicList()!=null && getBasicList().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RootBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RootBuilder o = (RootBuilder) other;
			
			merger.mergeRosetta(getBasicSingle(), o.getBasicSingle(), this::setBasicSingle);
			merger.mergeRosetta(getBasicList(), o.getBasicList(), this::setBasicList);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(basicSingle, _that.getBasicSingle())) return false;
			if (!Objects.equals(basicList, _that.getBasicList())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (basicSingle != null ? basicSingle.hashCode() : 0);
			_result = 31 * _result + (basicList != null ? basicList.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RootBuilder {" +
				"basicSingle=" + this.basicSingle + ", " +
				"basicList=" + this.basicList +
			'}';
		}
	}
}
