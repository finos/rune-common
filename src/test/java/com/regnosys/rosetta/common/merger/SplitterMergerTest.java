package com.regnosys.rosetta.common.merger;

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
