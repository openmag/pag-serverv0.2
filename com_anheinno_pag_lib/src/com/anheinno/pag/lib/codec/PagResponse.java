package com.anheinno.pag.lib.codec;

import org.jboss.netty.util.internal.StringUtil;

public class PagResponse extends PagMessage {
	
	public static final String RESPONSE_METHOD = "RSP";
	
    private PagResponseStatus _status = null;

	public PagResponse(PagVersion version, PagResponseStatus status) {
		super(version);
		setStatus(status);
	}

    public PagResponseStatus getStatus() {
        return _status;
    }

    public void setStatus(PagResponseStatus status) {
        if (status == null) {
            throw new NullPointerException("status");
        }
        this._status = status;
    }

	protected void appendFirstLine(StringBuilder buf) {
		buf.append(getProtocolVersion().toString());
		buf.append(' ');
		buf.append(_status.toString());
		buf.append(StringUtil.NEWLINE);
	}

}
