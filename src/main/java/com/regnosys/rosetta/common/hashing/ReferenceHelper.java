package com.regnosys.rosetta.common.hashing;

import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.RosettaModelObjectBuilder;

public interface ReferenceHelper {

	<T extends RosettaModelObject> void generateGlobalKeys(Class<T> topClass, RosettaModelObjectBuilder builder, boolean overwriteExisting);

	void replace();

	void refresh();


}
