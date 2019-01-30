package com.regnosys.rosetta.common.util;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class TestStreamUtils {
	
	private static class Tree {
		Collection<Tree> tests = new ArrayList<>();
	}
	
	@Test
	void testFlattenTree() {
		Tree t1 = new Tree();
		Tree t2 = new Tree();
		Tree t3 = new Tree();
		Tree t4 = new Tree();
		t1.tests.add(t2);
		t1.tests.add(t3);
		t2.tests.add(t4);
		t4.tests.add(t1);//this tree contains a loop
		
		List<Tree> result = StreamUtils.flattenTreeC(t1, t->t.tests).collect(Collectors.toList());
		assertEquals(4,  result.size());
	}

}
