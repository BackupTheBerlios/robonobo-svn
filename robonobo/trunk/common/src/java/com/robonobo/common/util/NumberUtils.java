package com.robonobo.common.util;

public class NumberUtils {
	/** Never instantiate this class
	 */
	private NumberUtils() {
	}
	
	public static final boolean dblEq(double a, double b) {
		return (a - b) == 0d;
	}
}
