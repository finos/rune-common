package com.regnosys.rosetta.common.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.function.BiPredicate;

public class CollectionUtils {

	/**
	 * Tests if two lists contain matching elements in the same order
	 * 
	 * @param list1
	 *            A list
	 * @param list2
	 *            A list
	 * @param comparer
	 *            A BiPredicate that compares two objects and returns true if they
	 *            "match"
	 * @return
	 */
	public static <A> boolean listMatch(List<A> list1, List<A> list2, BiPredicate<A, A> comparer) {
		if (list1.size() != list2.size())
			return false;
		Iterator<A> it1 = list1.iterator();
		for (Iterator<A> it2 = list2.iterator(); it2.hasNext();) {
			A a2 = it2.next();
			A a1 = it1.next();
			if (!comparer.test(a1, a2))
				return false;
		}
		return true;
	}

	/**
	 * Tests if two collections contain matching elements (ignoring ordering)
	 * 
	 * @param col1
	 *            A collection
	 * @param col2
	 *            A collection
	 * @param comparer
	 *            A BiPredicate that compares two objects and returns true if they
	 *            "match"
	 * @return
	 */
	public static <A> boolean collectionContains(Collection<A> col1, Collection<A> col2, BiPredicate<A, A> comparer) {
		List<A> tempCol = new LinkedList<>(col2);
		for (A a1 : col1) {
			Iterator<A> it = tempCol.iterator();
			boolean found = false;
			while (!found && it.hasNext()) {
				A a2 = it.next();
				if (comparer.test(a1, a2)) {
					found = true;
					it.remove();
				}
			}
			if (!found)
				return false;
		}
		return true;
	}
}
