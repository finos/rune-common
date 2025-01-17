package com.regnosys.rosetta.common.transform;

/*-
 * ==============
 * Rune Common
 * ==============
 * Copyright (C) 2018 - 2024 REGnosys
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

import com.google.common.base.CaseFormat;
import com.google.common.collect.Iterables;
import com.rosetta.model.lib.annotations.RosettaReport;
import com.rosetta.model.lib.functions.RosettaFunction;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class FunctionNameHelper {

    public Class<?> getInputClass(Class<? extends RosettaFunction> function) {
        Method functionMethod = getFuncMethod(function);
        return functionMethod.getParameterTypes()[0];
    }

    public String getInputType(Class<? extends RosettaFunction> function) {
        return getInputClass(function).getName();
    }

    public String getOutputType(Class<? extends RosettaFunction> function) {
        Method functionMethod = getFuncMethod(function);
        return functionMethod.getReturnType().getName();
    }

    public Method getFuncMethod(Class<? extends RosettaFunction> function) {
        try {
            List<Method> evaluateMethods = Arrays.stream(function.getMethods())
                    .filter(x -> x.getName().equals("evaluate"))
                    .collect(Collectors.toList());
            return Iterables.getLast(evaluateMethods);
        } catch (Exception ex) {
            throw new EvaluateFunctionNotFoundException("evaluate method not found in " + function.getName(), ex);
        }

    }

    public String getName(Class<? extends RosettaFunction> function) {
        return Optional.ofNullable(function.getAnnotation(com.rosetta.model.lib.annotations.RosettaReport.class))
                .map(a -> String.format("%s / %s", a.body(), String.join(" ", a.corpusList())))
                .orElse(readableFunctionName(function));
    }

    public String capitalizeFirstLetter(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }
        // Capitalize the first letter and concatenate with the rest of the string
        return input.substring(0, 1).toUpperCase() + input.substring(1);
    }

    public String readableId(Class<? extends RosettaFunction> function) {
        String simpleName = Optional.ofNullable(function.getAnnotation(RosettaReport.class))
                .map(a -> String.format("%s-%s", a.body(), String.join("-", a.corpusList())))
                .orElse(function.getSimpleName());

        return readableId(simpleName);
    }

    private String readableId(String simpleName) {

        String sanitise = simpleName
                .replace("Ingest_", "")
                .replace("Report_", "")
                .replace("Function", "")
                .replace("Enrich_", "")
                .replace("Project_", "")
                .replace("-", ".")
                .replace("_", ".");

        String functionName = lowercaseConsecutiveUppercase(sanitise)
                .replace(".", "");

        return CaseFormat.UPPER_CAMEL
                .converterTo(CaseFormat.LOWER_HYPHEN)
                .convert(functionName);
    }

    public String readableFunctionName(String functionSimpleName){
        return readableFunctionNameFromId(readableId(functionSimpleName));
    }

    private String readableFunctionNameFromId(String readableId) {
        return Arrays.stream(readableId.split("-"))
                .map(s -> s.substring(0, 1).toUpperCase() + s.substring(1))
                .collect(Collectors.joining(" "));
    }

    private String readableFunctionName(Class<? extends RosettaFunction> function) {
        String readableId = readableId(function);

        return readableFunctionNameFromId(readableId);
    }

    private String lowercaseConsecutiveUppercase(String input) {
        StringBuilder result = new StringBuilder();
        boolean inUppercaseSequence = false;
        for (int i = 0; i < input.length(); i++) {
            char currentChar = input.charAt(i);
            char newChar = currentChar;
            boolean isLastChar = i == input.length() - 1;
            if (Character.isUpperCase(currentChar)) {
                if (!inUppercaseSequence) {
                    // Append the first uppercase character
                    inUppercaseSequence = true;
                } else if (isLastChar || Character.isUpperCase(input.charAt(i + 1)) || input.charAt(i + 1) == '.') {
                    newChar = Character.toLowerCase(currentChar);
                    // Lowercase the middle characters
                } else {
                    // Append the last uppercase character
                    inUppercaseSequence = false;
                }
            } else {
                // Append lowercase or non-letter characters
                inUppercaseSequence = false;
            }
            result.append(newChar);
        }
        return result.toString();
    }

}
