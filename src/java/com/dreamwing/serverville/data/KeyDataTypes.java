package com.dreamwing.serverville.data;

// Do not reorder - only add to end
public enum KeyDataTypes {
	NULL(0),
	FALSE(1),
	TRUE(2),
	BYTE(3),
	BYTE_ZERO(4),
	BYTE_ONE(5),
	SHORT(6),
	SHORT_ZERO(7),
	SHORT_ONE(8),
	INT(9),
	INT_ZERO(10),
	INT_ONE(11),
	LONG(12),
	LONG_ZERO(13),
	LONG_ONE(14),
	FLOAT(15),
	FLOAT_ZERO(16),
	FLOAT_ONE(17),
	DOUBLE(18),
	DOUBLE_ZERO(19),
	DOUBLE_ONE(20),
	STRING(21),
	STRING_JSON(22),
	STRING_XML(23),
	DATETIME(24),
	LIST(25),
	DICT(26),
	BYTES(27),
	JAVA_SERIALIZED(28);
	
	private int id;
	private static KeyDataTypes[] ValueLookup;
	
	static
	{
		ValueLookup = new KeyDataTypes[29];
		KeyDataTypes[] values = KeyDataTypes.values();
		for(KeyDataTypes val : values)
		{
			ValueLookup[val.id] = val;
		}
	}
	
	private KeyDataTypes(int i)
	{
		id = i;
	}
	
	public int toInt() { return id;}
	
	
	public static KeyDataTypes fromInt(int id)
	{
		return ValueLookup[id];
	}
}
