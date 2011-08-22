package com.anheinno.pag.lib.codec;

import org.jboss.netty.util.internal.StringUtil;

public class PagRequest extends PagMessage {
	
	private String _dest = null;
    private PagMethod _method = null;

    public PagRequest(final PagVersion version, final PagMethod method) {
    	super(version);
    	setMethod(method);
    }
    
    public PagRequest(final PagVersion version, final PagMethod method, final String dest) {
    	super(version);
    	setMethod(method);
    	setDestination(dest);
    }
    
    public PagMethod getMethod() {
        return _method;
    }

    public void setMethod(PagMethod method) {
        if (method == null) {
            throw new NullPointerException("method");
        }
        this._method = method;
    }
    
    public boolean hasDestination() {
    	if(_dest != null && _dest.length() > 0) {
    		return true;
    	}else {
    		return false;
    	}
    }
    
    public String getDestination() {
    	return _dest;
    }
    
    public void setDestination(String id) {
    	if(id == null) {
    		throw new NullPointerException("id");
    	}
    	this._dest = id;
    }

	protected void appendFirstLine(StringBuilder buf) {
		buf.append(_method.toString());
		buf.append(' ');
		buf.append(getDestination());
		buf.append(' ');
		buf.append(getProtocolVersion().toString());
		buf.append(StringUtil.NEWLINE);
	}

}
