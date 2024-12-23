package org.finos.rune.serialization;

import java.util.Map;

public class DynamicCompiledClassLoader extends ClassLoader {
    private Map<String, Class<?>> compiledCode;

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        return compiledCode.containsKey(name) ? compiledCode.get(name) : super.findClass(name);
    }

    public void setCompiledCode(Map<String, Class<?>> compiledCode) {
        this.compiledCode = compiledCode;
    }
}
