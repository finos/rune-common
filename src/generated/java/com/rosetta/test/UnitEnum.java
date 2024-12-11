package com.rosetta.test;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import com.rosetta.test.UnitEnum;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @version ${project.version}
 */
@RosettaEnum("UnitEnum")
public enum UnitEnum {

	@RosettaEnumValue(value = "METER") 
	METER("METER", null),
	
	@RosettaEnumValue(value = "KILOGRAM") 
	KILOGRAM("KILOGRAM", null)
;
	private static Map<String, UnitEnum> values;
	static {
        Map<String, UnitEnum> map = new ConcurrentHashMap<>();
		for (UnitEnum instance : UnitEnum.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	UnitEnum(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static UnitEnum fromDisplayName(String name) {
		UnitEnum value = values.get(name);
		if (value == null) {
			throw new IllegalArgumentException("No enum constant with display name \"" + name + "\".");
		}
		return value;
	}

	@Override
	public String toString() {
		return toDisplayString();
	}

	public String toDisplayString() {
		return displayName != null ?  displayName : rosettaName;
	}
}
