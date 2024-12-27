package test.metascheme;

import com.rosetta.model.lib.annotations.RosettaEnum;
import com.rosetta.model.lib.annotations.RosettaEnumValue;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


/**
 * @version 0.0.0
 */
@RosettaEnum("EnumType")
public enum EnumType {

	@RosettaEnumValue(value = "A") 
	A("A", null),
	
	@RosettaEnumValue(value = "B") 
	B("B", null),
	
	@RosettaEnumValue(value = "C") 
	C("C", null)
;
	private static Map<String, EnumType> values;
	static {
        Map<String, EnumType> map = new ConcurrentHashMap<>();
		for (EnumType instance : EnumType.values()) {
			map.put(instance.toDisplayString(), instance);
		}
		values = Collections.unmodifiableMap(map);
    }

	private final String rosettaName;
	private final String displayName;

	EnumType(String rosettaName, String displayName) {
		this.rosettaName = rosettaName;
		this.displayName = displayName;
	}

	public static EnumType fromDisplayName(String name) {
		EnumType value = values.get(name);
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
