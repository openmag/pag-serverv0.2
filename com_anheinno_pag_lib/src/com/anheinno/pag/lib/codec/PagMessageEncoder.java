/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package com.anheinno.pag.lib.codec;


import java.io.UnsupportedEncodingException;
import java.util.Map;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.oneone.OneToOneEncoder;
import org.jboss.netty.util.CharsetUtil;

public class PagMessageEncoder extends OneToOneEncoder {
	
    public PagMessageEncoder() {
        super();
    }

    @Override
    protected Object encode(ChannelHandlerContext ctx, Channel channel, Object msg) throws Exception {
        if (msg instanceof PagMessage) {
            PagMessage m = (PagMessage) msg;
            ChannelBuffer content = m.getContent();
            
            ChannelBuffer header = ChannelBuffers.dynamicBuffer(channel.getConfig().getBufferFactory());
            encodeInitialLine(header, m);
            encodeHeaders(header, m);
            if(content.readable() && !m.containsHeader(HttpHeaders.Names.CONTENT_LENGTH)) {
            	encodeHeader(header, HttpHeaders.Names.CONTENT_LENGTH, "" + content.toString(CharsetUtil.UTF_8).length());
            }
            header.writeByte(PagCodecUtil.CR);
            header.writeByte(PagCodecUtil.LF);
            
            if (!content.readable()) {
                return header; // no content
            } else {
                return ChannelBuffers.wrappedBuffer(header, content);
            }
        }
        // Unknown message type.
        return msg;
    }

    private void encodeHeaders(ChannelBuffer buf, PagMessage message) {
        try {
            for (Map.Entry<String, String> h: message.getHeaders()) {
                encodeHeader(buf, h.getKey(), h.getValue());
            }
        } catch (UnsupportedEncodingException e) {
            throw (Error) new Error().initCause(e);
        }
    }

    private void encodeHeader(ChannelBuffer buf, String header, String value)
            throws UnsupportedEncodingException {
        buf.writeBytes(header.getBytes("ASCII"));
        buf.writeByte(PagCodecUtil.COLON);
        buf.writeByte(PagCodecUtil.SP);
        buf.writeBytes(value.getBytes("ASCII"));
        buf.writeByte(PagCodecUtil.CR);
        buf.writeByte(PagCodecUtil.LF);
    }

    private void encodeInitialLine(ChannelBuffer buf, PagMessage message) throws Exception {
    	if(message instanceof PagRequest) {
    		PagRequest req = (PagRequest)message;
        	buf.writeBytes(req.getMethod().toString().getBytes(CharsetUtil.US_ASCII));
        	if(req.hasDestination()) {
        		buf.writeByte(PagCodecUtil.SP);
        		buf.writeBytes(req.getDestination().getBytes(CharsetUtil.US_ASCII));
        	}
        	buf.writeByte(PagCodecUtil.SP);
        	buf.writeBytes(req.getProtocolVersion().toString().getBytes(CharsetUtil.US_ASCII));
        	
    	}else if(message instanceof PagResponse) {
    		PagResponse resp = (PagResponse)message;
    		/*
    		buf.writeBytes(resp.getProtocolVersion().toString().getBytes(CharsetUtil.US_ASCII));
	        buf.writeByte(PagCodecUtil.SP);
	        buf.writeBytes(String.valueOf(resp.getStatus().getCode()).getBytes(CharsetUtil.US_ASCII));
	        buf.writeByte(PagCodecUtil.SP);
	        buf.writeBytes(String.valueOf(resp.getStatus().getReasonPhrase()).getBytes(CharsetUtil.US_ASCII));
	        */
    		buf.writeBytes(PagResponse.RESPONSE_METHOD.getBytes(CharsetUtil.US_ASCII));
    		buf.writeByte(PagCodecUtil.SP);
	        buf.writeBytes(String.valueOf(resp.getStatus().getCode()).getBytes(CharsetUtil.US_ASCII));
	        buf.writeByte(PagCodecUtil.SP);
    		buf.writeBytes(resp.getProtocolVersion().toString().getBytes(CharsetUtil.US_ASCII));	        
    		buf.writeByte(PagCodecUtil.SP);
	        buf.writeBytes(resp.getStatus().getReasonPhrase().getBytes(CharsetUtil.US_ASCII));
    	}
        buf.writeByte(PagCodecUtil.CR);
        buf.writeByte(PagCodecUtil.LF);   	
    }
}
