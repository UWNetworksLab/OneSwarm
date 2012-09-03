package edu.washington.cs.oneswarm.f2f.xml;

public class Utils {
	public static int lastIndexOf(byte[] haystack, byte[] needle) {
		int needleLength = needle.length;
		for (int i = haystack.length - needleLength; i > 0; i--) {
			if (haystack[i] == needle[0]) {
				boolean match = true;
				for (int j = 1; j < needleLength; j++) {
					if (haystack[i+j] != needle[j]) {
						match = false;
						break;
					}
				}
				if (match) {
					return i;
				}
			}
		}
		return -1;
	}
}
