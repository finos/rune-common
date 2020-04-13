package com.regnosys.rosetta.common.testing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.net.URL;
import java.util.List;

public class ExecutionDescriptor {

    private String group;
    private String name;
    private String description;
    private String inputFile;
    private String expectedOutputFile;
    private String executableFunctionClass;
    private boolean nativeFunction;

    public static List<ExecutionDescriptor> loadExecutionDescriptor(ObjectMapper objectMapper, URL url) {
        try {
            return objectMapper.readValue(url, new TypeReference<List<ExecutionDescriptor>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to load expectations from " + url.toString(), e);
        }
    }

    public ExecutionDescriptor() {
    }

    public ExecutionDescriptor(String group, String name, String description, String inputFile, String expectedOutputFile, String executableFunctionClass, boolean nativeFunction) {
        this.group = group;
        this.name = name;
        this.description = description;
        this.inputFile = inputFile;
        this.expectedOutputFile = expectedOutputFile;
        this.executableFunctionClass = executableFunctionClass;
        this.nativeFunction = nativeFunction;
    }

    public boolean isNativeFunction() {
        return nativeFunction;
    }

    public String getGroup() {
        return group;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getInputFile() {
        return inputFile;
    }

    public String getExpectedOutputFile() {
        return expectedOutputFile;
    }

    public String getExecutableFunctionClass() {
        return executableFunctionClass;
    }
}
