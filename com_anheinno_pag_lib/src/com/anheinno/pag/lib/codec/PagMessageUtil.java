package com.anheinno.pag.lib.codec;

public class PagMessageUtil {
	
	private PagMessageUtil() {
		super();
	}
	
	public static String getID(PagMessage msg) {
		return msg.getHeader(PagHeaders.Names.ID);
	}
}
