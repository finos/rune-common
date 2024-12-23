package annotations;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.rosetta.model.lib.RosettaModelObject;
import com.rosetta.model.lib.annotations.RuneAttribute;

@JsonFilter("SubTypeFilter")
public @interface RosettaModelObjectMixin {

    @RuneAttribute("@type")
    Class<? extends RosettaModelObject> getType();

}
