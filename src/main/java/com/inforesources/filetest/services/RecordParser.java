package com.inforesources.filetest.services;

public class RecordParser {

	public static String[] parse(String s, char separator) {
		return s.split("\\s*" + separator + "\\s*");
	}
}
