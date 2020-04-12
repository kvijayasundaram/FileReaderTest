package com.inforesources.filetest.objects;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString(callSuper = true)
@AllArgsConstructor
public class TestRecord {
	private long id;
	private String name;

	public static TestRecord parse(String s, char separator) {
		String[] tokens = s.split("\\s*" + separator + "\\s*");
		if (tokens.length > 1)
			return new TestRecord(Long.parseLong(tokens[0]), tokens[1]);
		else
			return new TestRecord(0L, "Parsing Error");

	}
}
