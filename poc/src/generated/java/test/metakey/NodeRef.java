package test.metakey;

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

import test.metakey.A.ABuilder;
import test.metakey.NodeRef.NodeRefBuilderImpl;
import test.metakey.meta.NodeRefMeta;
import test.metakey.metafields.ReferenceWithMetaA;
import test.metakey.metafields.ReferenceWithMetaA.ReferenceWithMetaABuilder;

import static java.util.Optional.ofNullable;

/**
 * @version 0.0.0
 */
@RosettaDataType(value="NodeRef", builder= NodeRefBuilderImpl.class, version="0.0.0")
@RuneDataType(value="NodeRef", model = "test", builder= NodeRefBuilderImpl.class, version="0.0.0")
public interface NodeRef extends RosettaModelObject {

	NodeRefMeta metaData = new NodeRefMeta();

	/*********************** Getter Methods  ***********************/
	A getTypeA();
	ReferenceWithMetaA getAReference();

	/*********************** Build Methods  ***********************/
	NodeRef build();
	
	NodeRefBuilder toBuilder();
	
	static NodeRefBuilder builder() {
		return new NodeRefBuilderImpl();
	}

	/*********************** Utility Methods  ***********************/
	@Override
	default RosettaMetaData<? extends NodeRef> metaData() {
		return metaData;
	}
	
	@Override
	@RuneAttribute("@type")
	default Class<? extends NodeRef> getType() {
		return NodeRef.class;
	}
	
	@Override
	default void process(RosettaPath path, Processor processor) {
		processRosetta(path.newSubPath("typeA"), processor, A.class, getTypeA());
		processRosetta(path.newSubPath("aReference"), processor, ReferenceWithMetaA.class, getAReference());
	}
	

	/*********************** Builder Interface  ***********************/
	interface NodeRefBuilder extends NodeRef, RosettaModelObjectBuilder {
		ABuilder getOrCreateTypeA();
		@Override
		ABuilder getTypeA();
		ReferenceWithMetaABuilder getOrCreateAReference();
		@Override
		ReferenceWithMetaABuilder getAReference();
		NodeRefBuilder setTypeA(A typeA);
		NodeRefBuilder setAReference(ReferenceWithMetaA aReference);
		NodeRefBuilder setAReferenceValue(A aReference);

		@Override
		default void process(RosettaPath path, BuilderProcessor processor) {
			processRosetta(path.newSubPath("typeA"), processor, ABuilder.class, getTypeA());
			processRosetta(path.newSubPath("aReference"), processor, ReferenceWithMetaABuilder.class, getAReference());
		}
		

		NodeRefBuilder prune();
	}

	/*********************** Immutable Implementation of NodeRef  ***********************/
	class NodeRefImpl implements NodeRef {
		private final A typeA;
		private final ReferenceWithMetaA aReference;
		
		protected NodeRefImpl(NodeRefBuilder builder) {
			this.typeA = ofNullable(builder.getTypeA()).map(f->f.build()).orElse(null);
			this.aReference = ofNullable(builder.getAReference()).map(f->f.build()).orElse(null);
		}
		
		@Override
		@RosettaAttribute("typeA")
		@RuneAttribute("typeA")
		public A getTypeA() {
			return typeA;
		}
		
		@Override
		@RosettaAttribute("aReference")
		@RuneAttribute("aReference")
		public ReferenceWithMetaA getAReference() {
			return aReference;
		}
		
		@Override
		public NodeRef build() {
			return this;
		}
		
		@Override
		public NodeRefBuilder toBuilder() {
			NodeRefBuilder builder = builder();
			setBuilderFields(builder);
			return builder;
		}
		
		protected void setBuilderFields(NodeRefBuilder builder) {
			ofNullable(getTypeA()).ifPresent(builder::setTypeA);
			ofNullable(getAReference()).ifPresent(builder::setAReference);
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			NodeRef _that = getType().cast(o);
		
			if (!Objects.equals(typeA, _that.getTypeA())) return false;
			if (!Objects.equals(aReference, _that.getAReference())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (typeA != null ? typeA.hashCode() : 0);
			_result = 31 * _result + (aReference != null ? aReference.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "NodeRef {" +
				"typeA=" + this.typeA + ", " +
				"aReference=" + this.aReference +
			'}';
		}
	}

	/*********************** Builder Implementation of NodeRef  ***********************/
	class NodeRefBuilderImpl implements NodeRefBuilder {
	
		protected ABuilder typeA;
		protected ReferenceWithMetaABuilder aReference;
		
		@Override
		@RosettaAttribute("typeA")
		@RuneAttribute("typeA")
		public ABuilder getTypeA() {
			return typeA;
		}
		
		@Override
		public ABuilder getOrCreateTypeA() {
			ABuilder result;
			if (typeA!=null) {
				result = typeA;
			}
			else {
				result = typeA = A.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("aReference")
		@RuneAttribute("aReference")
		public ReferenceWithMetaABuilder getAReference() {
			return aReference;
		}
		
		@Override
		public ReferenceWithMetaABuilder getOrCreateAReference() {
			ReferenceWithMetaABuilder result;
			if (aReference!=null) {
				result = aReference;
			}
			else {
				result = aReference = ReferenceWithMetaA.builder();
			}
			
			return result;
		}
		
		@Override
		@RosettaAttribute("typeA")
		@RuneAttribute("typeA")
		public NodeRefBuilder setTypeA(A _typeA) {
			this.typeA = _typeA == null ? null : _typeA.toBuilder();
			return this;
		}
		
		@Override
		@RosettaAttribute("aReference")
		@RuneAttribute("aReference")
		public NodeRefBuilder setAReference(ReferenceWithMetaA _aReference) {
			this.aReference = _aReference == null ? null : _aReference.toBuilder();
			return this;
		}
		
		@Override
		public NodeRefBuilder setAReferenceValue(A _aReference) {
			this.getOrCreateAReference().setValue(_aReference);
			return this;
		}
		
		@Override
		public NodeRef build() {
			return new NodeRefImpl(this);
		}
		
		@Override
		public NodeRefBuilder toBuilder() {
			return this;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public NodeRefBuilder prune() {
			if (typeA!=null && !typeA.prune().hasData()) typeA = null;
			if (aReference!=null && !aReference.prune().hasData()) aReference = null;
			return this;
		}
		
		@Override
		public boolean hasData() {
			if (getTypeA()!=null && getTypeA().hasData()) return true;
			if (getAReference()!=null && getAReference().hasData()) return true;
			return false;
		}
	
		@SuppressWarnings("unchecked")
		@Override
		public NodeRefBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
			NodeRefBuilder o = (NodeRefBuilder) other;
			
			merger.mergeRosetta(getTypeA(), o.getTypeA(), this::setTypeA);
			merger.mergeRosetta(getAReference(), o.getAReference(), this::setAReference);
			
			return this;
		}
	
		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || !(o instanceof RosettaModelObject) || !getType().equals(((RosettaModelObject)o).getType())) return false;
		
			NodeRef _that = getType().cast(o);
		
			if (!Objects.equals(typeA, _that.getTypeA())) return false;
			if (!Objects.equals(aReference, _that.getAReference())) return false;
			return true;
		}
		
		@Override
		public int hashCode() {
			int _result = 0;
			_result = 31 * _result + (typeA != null ? typeA.hashCode() : 0);
			_result = 31 * _result + (aReference != null ? aReference.hashCode() : 0);
			return _result;
		}
		
		@Override
		public String toString() {
			return "NodeRefBuilder {" +
				"typeA=" + this.typeA + ", " +
				"aReference=" + this.aReference +
			'}';
		}
	}
}
