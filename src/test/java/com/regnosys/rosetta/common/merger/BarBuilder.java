package com.regnosys.rosetta.common.merger;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;

public class BarBuilder implements RosettaModelObjectBuilder {

	Integer num;
	
	public Integer getNum() {
		return num;
	}

	public BarBuilder setNum(Integer num) {
		this.num = num;
		return this;
	}

	@Override
	public RosettaModelObjectBuilder toBuilder() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method toBuilder in BarBuilder has not been implemented");
	}

	@Override
	public RosettaModelObject build() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method build in BarBuilder has not been implemented");
	}

	@Override
	public RosettaMetaData<? extends RosettaModelObject> metaData() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method metaData in BarBuilder has not been implemented");
	}

	@Override
	public Class<? extends RosettaModelObject> getType() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method getType in BarBuilder has not been implemented");
	}

	@Override
	public void process(RosettaPath path, Processor processor) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method process in BarBuilder has not been implemented");
	}

	@Override
	public BarBuilder prune() {
		return this;
	}

	@Override
	public void process(RosettaPath path, BuilderProcessor processor) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method process in BarBuilder has not been implemented");
	}

	@Override
	public boolean hasData() {
		return num!=null;
	}

	@Override
	public BarBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
		BarBuilder ob = (BarBuilder) other;
		merger.mergeBasic(getNum(), ob.getNum(), this::setNum);
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((num == null) ? 0 : num.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		BarBuilder other = (BarBuilder) obj;
		if (num == null) {
			if (other.num != null)
				return false;
		} else if (!num.equals(other.num))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "BarBuilder [num=" + num + "]";
	}

}
