package annotations;

import com.rosetta.model.lib.RosettaModelObjectBuilder;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RuneDataType {
    String value() default "";

    Class<? extends RosettaModelObjectBuilder> builder();

    String version() default "";

    String model() default "";
}
