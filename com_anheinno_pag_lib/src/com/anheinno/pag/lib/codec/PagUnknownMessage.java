package com.anheinno.pag.lib.codec;

import org.jboss.netty.util.internal.StringUtil;

public class PagUnknownMessage extends PagMessage {
	
	public static PagUnknownMessage UNKNOWN;
	static {
		UNKNOWN = new PagUnknownMessage(PagVersion.PAA_1_0);
	}

	private PagUnknownMessage(PagVersion version) {
		super(version);
		// do nothing
	}

	@Override
	protected void appendFirstLine(StringBuilder buf) {
		buf.append("Unknown PagMessage");
		buf.append(StringUtil.NEWLINE);
	}

}
