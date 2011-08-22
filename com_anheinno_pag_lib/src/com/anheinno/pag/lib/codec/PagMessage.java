package com.anheinno.pag.lib.codec;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.internal.StringUtil;

public abstract class PagMessage {


	
    private final PagHeaders headers = new PagHeaders();
    private PagVersion version = null;
    private ChannelBuffer content = ChannelBuffers.EMPTY_BUFFER;
    private boolean _compress;

    protected PagMessage(final PagVersion version) {
        setProtocolVersion(version);
        setCompress(false);
    }
    
    public void addHeader(final String name, final Object value) {
        headers.addHeader(name, value);
    }

    public void setHeader(final String name, final Object value) {
        headers.setHeader(name, value);
    }

    public void setHeader(final String name, final Iterable<?> values) {
        headers.setHeader(name, values);
    }

    public void removeHeader(final String name) {
        headers.removeHeader(name);
    }

    public void clearHeaders() {
        headers.clearHeaders();
    }

    public void setContent(ChannelBuffer content) {
        if (content == null) {
            content = ChannelBuffers.EMPTY_BUFFER;
        }
        this.content = content;
    }

    public String getHeader(final String name) {
        List<String> values = getHeaders(name);
        return values.size() > 0 ? values.get(0) : null;
    }

    public List<String> getHeaders(final String name) {
        return headers.getHeaders(name);
    }

    public List<Map.Entry<String, String>> getHeaders() {
        return headers.getHeaders();
    }

    public boolean containsHeader(final String name) {
        return headers.containsHeader(name);
    }

    public Set<String> getHeaderNames() {
        return headers.getHeaderNames();
    }
    
    public void setCompress(boolean compress) {
    	_compress = compress;
    }
    
    public boolean getCompress() {
    	return _compress;
    }

    public PagVersion getProtocolVersion() {
        return version;
    }

    public void setProtocolVersion(PagVersion version) {
        if (version == null) {
            throw new NullPointerException("version");
        }
        this.version = version;
    }

    public ChannelBuffer getContent() {
        return content;
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder();
        appendFirstLine(buf);
        appendHeaders(buf);

        // Remove the last newline.
        buf.setLength(buf.length() - StringUtil.NEWLINE.length());
        return buf.toString();
    }
    
    protected abstract void appendFirstLine(StringBuilder buf);

    private void appendHeaders(StringBuilder buf) {
        for (Map.Entry<String, String> e: getHeaders()) {
            buf.append(e.getKey());
            buf.append(": ");
            buf.append(e.getValue());
            buf.append(StringUtil.NEWLINE);
        }
    }

}
