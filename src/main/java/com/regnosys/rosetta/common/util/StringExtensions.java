package com.regnosys.rosetta.common.util;

import static java.lang.Character.toLowerCase;
import static java.lang.Character.toUpperCase;

public class StringExtensions {

    public static String toFirstLower(String s) {
        return s != null ? toLowerCase(s.charAt(0)) + s.substring(1) : "";
    }

    public static String toFirstUpper(String s) {
        return s != null ? toUpperCase(s.charAt(0)) + s.substring(1) : "";
    }
}

