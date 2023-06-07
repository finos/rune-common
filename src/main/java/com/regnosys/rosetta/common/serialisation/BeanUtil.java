package com.regnosys.rosetta.common.serialisation;

import java.lang.reflect.Method;

public class BeanUtil {

    public static String getPropertyName(Method method) {
        String methodName = method.getName();
        String rawPropertyName = getSubstringIfPrefixMatches(methodName, "get");
        if (rawPropertyName == null) {
            rawPropertyName = getSubstringIfPrefixMatches(methodName, "set");
        }

        if (rawPropertyName == null) {
            rawPropertyName = getSubstringIfPrefixMatches(methodName, "is");
        }

        if (rawPropertyName == null) {
            rawPropertyName = getSubstringIfPrefixMatches(methodName, "add");
        }

        return toLowerCamelCase(rawPropertyName);
    }

    public static String toLowerCamelCase(String string) {
        if (string == null) {
            return null;
        } else if (string.isEmpty()) {
            return string;
        } else if (string.length() > 1 && Character.isUpperCase(string.charAt(1)) && Character.isUpperCase(string.charAt(0))) {
            return string;
        } else {
            char[] chars = string.toCharArray();
            chars[0] = Character.toLowerCase(chars[0]);
            return new String(chars);
        }
    }

    private static String getSubstringIfPrefixMatches(String wholeString, String prefix) {
        return wholeString.startsWith(prefix) ? wholeString.substring(prefix.length()) : null;
    }
}
