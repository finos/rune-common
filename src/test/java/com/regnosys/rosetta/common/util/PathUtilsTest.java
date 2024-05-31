package com.regnosys.rosetta.common.util;

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

import com.regnosys.rosetta.common.translation.Path;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;

class PathUtilsTest {

	@Test
	void shouldFilterPaths() {
		Path p1 = Path.parse("d.e.f.g");
		Path p2 = Path.parse("a.b.c.d.e.f.g");
		Path p3 = Path.parse("x.y.z");
		Path p4 = Path.parse("g");
		Path p5 = Path.parse("a.b");
		Path p6 = Path.parse("f.g");
		Path p7 = Path.parse("y.z");
		Path p8 = Path.parse("d.e.f");

		List<Path> filteredPaths = PathUtils.filterSubPaths(Arrays.asList(p1, p2, p3, p4, p5, p6, p7, p8));

		assertThat(filteredPaths, hasSize(4));
		assertThat(filteredPaths, hasItems(p2, p3, p5, p8));
	}

	@Test
	void shouldFilterPathsWithMultipleCardinality() {
		Path p1 = Path.parse("a.b[0].c");
		Path p2 = Path.parse("b[0].c");
		Path p3 = Path.parse("a.b[1].c");
		Path p4 = Path.parse("b[1].c");

		List<Path> filteredPaths = PathUtils.filterSubPaths(Arrays.asList(p1, p2, p3, p4));

		assertThat(filteredPaths, hasSize(2));
		assertThat(filteredPaths, hasItems(p1, p3));
	}
}
