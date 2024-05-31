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

import static org.junit.jupiter.api.Assertions.*;

import com.regnosys.rosetta.common.merging.SimpleSplitter;
import org.junit.jupiter.api.Test;

class SplitterMergerTest {

	@Test
	void test() {
		FooBuilder before = new FooBuilder().setB1(new BarBuilder().setNum(1)).setB2(new BarBuilder().setNum(2));
		FooBuilder remove = new FooBuilder().setB1(new BarBuilder().setNum(1));
		
		FooBuilder after = new FooBuilder().setB2(new BarBuilder().setNum(2));
		
		SimpleSplitter splitter = new SimpleSplitter();
		
		FooBuilder result = before.merge(remove, splitter).prune();
		
		assertEquals(after, result);
		
	}

}
