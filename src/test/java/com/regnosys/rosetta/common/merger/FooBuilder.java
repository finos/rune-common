package com.regnosys.rosetta.common.merger;

/*-
 * ==============
 * Rosetta Common
 * --------------
 * Copyright (C) 2018 - 2024 REGnosys
 * --------------
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ==============
 */

/*-
 * #%L
 * Rosetta Common
 * %%
 * Copyright (C) 2018 - 2024 REGnosys
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.path.RosettaPath;
import com.rosetta.model.lib.process.BuilderMerger;
import com.rosetta.model.lib.process.BuilderProcessor;
import com.rosetta.model.lib.process.Processor;

public class FooBuilder implements RosettaModelObjectBuilder {

	
	BarBuilder b1;
	BarBuilder b2;
	
	public BarBuilder getB1() {
		return b1;
	}

	public FooBuilder setB1(BarBuilder b1) {
		this.b1 = b1;
		return this;
	}

	public BarBuilder getB2() {
		return b2;
	}

	public FooBuilder setB2(BarBuilder b2) {
		this.b2 = b2;
		return this;
	}

	@Override
	public RosettaModelObjectBuilder toBuilder() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method toBuilder in FooBuilder has not been implemented");
	}

	@Override
	public RosettaModelObject build() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method build in FooBuilder has not been implemented");
	}

	@Override
	public RosettaMetaData<? extends RosettaModelObject> metaData() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method metaData in FooBuilder has not been implemented");
	}

	@Override
	public Class<? extends RosettaModelObject> getType() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method getType in FooBuilder has not been implemented");
	}

	@Override
	public void process(RosettaPath path, Processor processor) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method process in FooBuilder has not been implemented");
	}

	@Override
	public FooBuilder prune() {
		if (b1!=null && !b1.prune().hasData()) b1 = null;
		if (b2!=null && !b2.prune().hasData()) b2 = null;
		return this;
	}

	@Override
	public void process(RosettaPath path, BuilderProcessor processor) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method process in FooBuilder has not been implemented");
	}

	@Override
	public boolean hasData() {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("method hasData in FooBuilder has not been implemented");
	}

	@Override
	public FooBuilder merge(RosettaModelObjectBuilder other, BuilderMerger merger) {
		FooBuilder fo = (FooBuilder)other;
		merger.mergeRosetta(getB1(), fo.getB1(), this::setB1);
		merger.mergeRosetta(getB2(), fo.getB2(), this::setB2);
		return this;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((b1 == null) ? 0 : b1.hashCode());
		result = prime * result + ((b2 == null) ? 0 : b2.hashCode());
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
		FooBuilder other = (FooBuilder) obj;
		if (b1 == null) {
			if (other.b1 != null)
				return false;
		} else if (!b1.equals(other.b1))
			return false;
		if (b2 == null) {
			if (other.b2 != null)
				return false;
		} else if (!b2.equals(other.b2))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "FooBuilder [b1=" + b1 + ", b2=" + b2 + "]";
	}

}
