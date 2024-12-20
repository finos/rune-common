package annotations;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.rosetta.model.lib.annotations.RuneAttribute;
import test.metakey.A;

@JsonFilter("SubTypeFilter")
public @interface RossetaModelObjectMixin {

    @RuneAttribute("@type")
    Class<? extends A> getType();

}
