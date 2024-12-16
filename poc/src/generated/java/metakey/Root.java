package metakey;

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
import metakey.AttributeRef;
import metakey.NodeRef;
import metakey.Root;
import metakey.Root.RootBuilder;
import metakey.Root.RootBuilderImpl;
import metakey.Root.RootImpl;
import metakey.meta.RootMeta;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="Root", builder= RootBuilderImpl.class, version="0.0.0")
public interface Root extends RosettaModelObject {

	RootMeta metaData = new RootMeta();

	/*********************** Getter Methods  ***********************/
	NodeRef getNodeRef();
	AttributeRef getAttributeRef();

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
		processRosetta(path.newSubPath("nodeRef"), processor, NodeRef.class, getNodeRef());
		processRosetta(path.newSubPath("attributeRef"), processor, AttributeRef.class, getAttributeRef());
	}
	

	/*********************** Builder Interface  ***********************/
	interface RootBuilder extends Root, RosettaModelObjectBuilder {
		NodeRef.NodeRefBuilder getOrCreateNodeRef();
		NodeRef.NodeRefBuilder getNodeRef();
		AttributeRef.AttributeRefBuilder getOrCreateAttributeRef();
		AttributeRef.AttributeRefBuilder getAttributeRef();
		RootBuilder setNodeRef(NodeRef nodeRef);
		RootBuilder setAttributeRef(AttributeRef attributeRef);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("nodeRef"), processor, NodeRef.NodeRefBuilder.class, getNodeRef());
			processRosetta(path.newSubPath("attributeRef"), processor, AttributeRef.AttributeRefBuilder.class, getAttributeRef());
		}
		

		RootBuilder prune();
	}

	/*********************** Immutable Implementation of Root  ***********************/
	class RootImpl implements Root {
		private final NodeRef nodeRef;
		private final AttributeRef attributeRef;
		
		protected RootImpl(RootBuilder builder) {
			this.nodeRef = ofNullable(builder.getNodeRef()).map(f->f.build()).orElse(null);
			this.attributeRef = ofNullable(builder.getAttributeRef()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("nodeRef")
		public NodeRef getNodeRef() {
			return nodeRef;
		}
		
		@Override
		@RosettaAttribute("attributeRef")
		public AttributeRef getAttributeRef() {
			return attributeRef;
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
			ofNullable(getNodeRef()).ifPresent(builder::setNodeRef);
			ofNullable(getAttributeRef()).ifPresent(builder::setAttributeRef);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(nodeRef, _that.getNodeRef())) return false;
			if (!Objects.equals(attributeRef, _that.getAttributeRef())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (nodeRef != null ? nodeRef.hashCode() : 0);
			_result = 31 * _result + (attributeRef != null ? attributeRef.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "Root {" +
				"nodeRef=" + this.nodeRef + ", " +
				"attributeRef=" + this.attributeRef +
			'}';
		}
	}

	/*********************** Builder Implementation of Root  ***********************/
	class RootBuilderImpl implements RootBuilder {
	
		protected NodeRef.NodeRefBuilder nodeRef;
		protected AttributeRef.AttributeRefBuilder attributeRef;
	
		public RootBuilderImpl() {
		}
	
		@Override
		@RosettaAttribute("nodeRef")
		public NodeRef.NodeRefBuilder getNodeRef() {
			return nodeRef;
		}
		
		@Override
		public NodeRef.NodeRefBuilder getOrCreateNodeRef() {
			NodeRef.NodeRefBuilder result;
			if (nodeRef!=null) {
				result = nodeRef;
			}
			else {
				result = nodeRef = NodeRef.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("attributeRef")
		public AttributeRef.AttributeRefBuilder getAttributeRef() {
			return attributeRef;
		}
		
		@Override
		public AttributeRef.AttributeRefBuilder getOrCreateAttributeRef() {
			AttributeRef.AttributeRefBuilder result;
			if (attributeRef!=null) {
				result = attributeRef;
			}
			else {
				result = attributeRef = AttributeRef.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("nodeRef")
		public RootBuilder setNodeRef(NodeRef nodeRef) {
			this.nodeRef = nodeRef==null?null:nodeRef.toBuilder();
			return this;
		}
		@Override
		@RosettaAttribute("attributeRef")
		public RootBuilder setAttributeRef(AttributeRef attributeRef) {
			this.attributeRef = attributeRef==null?null:attributeRef.toBuilder();
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
			if (nodeRef!=null && !nodeRef.prune().hasData()) nodeRef = null;
			if (attributeRef!=null && !attributeRef.prune().hasData()) attributeRef = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getNodeRef()!=null && getNodeRef().hasData()) return true;
			if (getAttributeRef()!=null && getAttributeRef().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public RootBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			RootBuilder o = (RootBuilder) other;
			
			merger.mergeRosetta(getNodeRef(), o.getNodeRef(), this::setNodeRef);
			merger.mergeRosetta(getAttributeRef(), o.getAttributeRef(), this::setAttributeRef);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			Root _that = getType().cast(o);
		
			if (!Objects.equals(nodeRef, _that.getNodeRef())) return false;
			if (!Objects.equals(attributeRef, _that.getAttributeRef())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (nodeRef != null ? nodeRef.hashCode() : 0);
			_result = 31 * _result + (attributeRef != null ? attributeRef.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "RootBuilder {" +
				"nodeRef=" + this.nodeRef + ", " +
				"attributeRef=" + this.attributeRef +
			'}';
		}
	}
}
