package com.regnosys.rosetta.common.serialisation.csv;


import com.rosetta.model.lib.annotations.RosettaMeta;
import com.rosetta.model.lib.meta.RosettaMetaData;
import com.rosetta.model.lib.qualify.QualifyFunctionFactory;
import com.rosetta.model.lib.qualify.QualifyResult;
import com.rosetta.model.lib.validation.Validator;
import com.rosetta.model.lib.validation.ValidatorFactory;
import com.rosetta.model.lib.validation.ValidatorWithArg;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * @version 0.0.0.master-SNAPSHOT
 */
@RosettaMeta(model=User.class)
public class UserMeta implements RosettaMetaData<User> {

    @Override
    public List<Validator<? super User>> dataRules(ValidatorFactory factory) {
        return Arrays.asList(
        );
    }

    @Override
    public List<Function<? super User, QualifyResult>> getQualifyFunctions(QualifyFunctionFactory factory) {
        return Collections.emptyList();
    }

    @Override
    public Validator<? super User> validator(ValidatorFactory factory) {
        return factory.<User>create(UserValidator.class);
    }

    @Override
    public Validator<? super User> typeFormatValidator(ValidatorFactory factory) {
        return factory.<User>create(UserTypeFormatValidator.class);
    }

    @Deprecated
    @Override
    public Validator<? super User> validator() {
        return new UserValidator();
    }

    @Deprecated
    @Override
    public Validator<? super User> typeFormatValidator() {
        return new UserTypeFormatValidator();
    }

    @Override
    public ValidatorWithArg<? super User, Set<String>> onlyExistsValidator() {
        return new UserOnlyExistsValidator();
    }
}
