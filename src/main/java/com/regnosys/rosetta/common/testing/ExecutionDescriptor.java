package com.regnosys.rosetta.common.testing;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

public class ExecutionDescriptor {

    private String group;
    private String name;
    private String description;
    private String markDownFile;
    private String inputFile;
    private String inputContent;
    private String expectedOutputFile;

    public ExecutionDescriptor(String name, String inputContent, String executableFunctionClass) {
        this.name = name;
        this.inputContent = inputContent;
        this.executableFunctionClass = executableFunctionClass;
        this.nativeFunction = true;
    }

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

    public static List<ExecutionDescriptor> loadExecutionDescriptor(ObjectMapper objectMapper, String resourceName, InputStream inputStream) {
        try {
            return objectMapper.readValue(inputStream, new TypeReference<List<ExecutionDescriptor>>() {
            });
        } catch (IOException e) {
            throw new RuntimeException("Unable to load expectations " + resourceName, e);
        }
    }

    public ExecutionDescriptor() {
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

    public String getInputContent() {
        return inputContent;
    }

    public String getExpectedOutputFile() {
        return expectedOutputFile;
    }

    public String getExecutableFunctionClass() {
        return executableFunctionClass;
    }

    public String getMarkDownFile() {
        return markDownFile;
    }
}
