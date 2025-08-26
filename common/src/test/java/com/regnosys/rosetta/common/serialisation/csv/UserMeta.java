package com.regnosys.rosetta.common.serialisation.csv;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2025 REGnosys
 * ==============
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
 * ==============
 */


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
