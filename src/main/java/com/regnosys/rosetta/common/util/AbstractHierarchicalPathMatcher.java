package com.regnosys.rosetta.common.util;

import com.regnosys.rosetta.common.util.HierarchicalPath.Element;

public abstract class AbstractHierarchicalPathMatcher implements HierarchicalPathMatcher {

	@Override
	public boolean matches(HierarchicalPath p1, HierarchicalPath p2) {
		if (p2 == null)
			return false;
		if (p1.getParent() != null ? !matches(p1.getParent(), p2.getParent()) : p2.getParent() != null)
			return false;
		return p1.getElement() != null ? matches(p1.getElement(), p2.getElement()) : p2.getElement() == null;
	}

	protected abstract boolean matches(Element e1, Element e2);

}
