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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.handler.codec.frame.TooLongFrameException;
import org.jboss.netty.handler.codec.replay.ReplayingDecoder;


public class PagMessageDecoder extends ReplayingDecoder<PagMessageDecoder.State> {

    private final int maxInitialLineLength;
    private final int maxHeaderSize;
    private PagMessage message;
    private ChannelBuffer content;
    private int headerSize;

    protected static enum State {
        SKIP_CONTROL_CHARS,
        READ_INITIAL,
        READ_HEADER,
        READ_FIXED_LENGTH_CONTENT,
    }

    /**
     * Creates a new instance with the default
     * {@code maxInitialLineLength (4096}}, {@code maxHeaderSize (8192)}, and
     * {@code maxChunkSize (8192)}.
     */
    public PagMessageDecoder() {
        this(4096, 8192);
    }

    /**
     * Creates a new instance with the specified parameters.
     */
    protected PagMessageDecoder(int maxInitialLineLength, int maxHeaderSize) {

        super(State.SKIP_CONTROL_CHARS, true);

        if (maxInitialLineLength <= 0) {
            throw new IllegalArgumentException(
                    "maxInitialLineLength must be a positive integer: " +
                    maxInitialLineLength);
        }
        if (maxHeaderSize <= 0) {
            throw new IllegalArgumentException(
                    "maxHeaderSize must be a positive integer: " +
                    maxHeaderSize);
        }
        this.maxInitialLineLength = maxInitialLineLength;
        this.maxHeaderSize = maxHeaderSize;
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, Channel channel, ChannelBuffer buffer, State state) throws Exception {
        switch (state) {
        case SKIP_CONTROL_CHARS: {
            try {
                skipControlCharacters(buffer);
                checkpoint(State.READ_INITIAL);
            } finally {
                checkpoint();
            }
        }
        case READ_INITIAL: {
            String[] initialLine = splitInitialLine(readLine(buffer, maxInitialLineLength));
            if (initialLine.length < 2) {
                // Invalid initial line - ignore.
                checkpoint(State.SKIP_CONTROL_CHARS);
                return null;
            }

            message = createMessage(initialLine);
            checkpoint(State.READ_HEADER);
        }
        case READ_HEADER: {
            readHeaders(buffer);
            checkpoint(State.READ_FIXED_LENGTH_CONTENT);
            long contentLength = PagHeaders.getContentLength(message, 0);
            if (contentLength == 0) {
            	content = ChannelBuffers.EMPTY_BUFFER;
                return reset();
            }

        }
        
        case READ_FIXED_LENGTH_CONTENT: {
            //we have a content-length so we just read the correct number of bytes
            readFixedLengthContent(buffer);
            return reset();
        }
        default: {
            throw new Error("Shouldn't reach here.");
        }

        }
    }

    private Object reset() {
        PagMessage message = this.message;
        ChannelBuffer content = this.content;

        if (content != null) {
            message.setContent(content);
            this.content = null;
        }
        this.message = null;

        checkpoint(State.SKIP_CONTROL_CHARS);
        return message;
    }

    private void skipControlCharacters(ChannelBuffer buffer) {
        for (;;) {
            char c = (char) buffer.readUnsignedByte();
            if (!Character.isISOControl(c) &&
                !Character.isWhitespace(c)) {
                buffer.readerIndex(buffer.readerIndex() - 1);
                break;
            }
        }
    }

    private void readFixedLengthContent(ChannelBuffer buffer) {
        long length = PagHeaders.getContentLength(message, -1);
        assert length <= Integer.MAX_VALUE;

        if (content == null) {
            content = buffer.readBytes((int) length);
        } else {
            content.writeBytes(buffer.readBytes((int) length));
        }
    }

    private State readHeaders(ChannelBuffer buffer) throws TooLongFrameException {
        headerSize = 0;
        final PagMessage message = this.message;
        String line = readHeader(buffer);
        String name = null;
        String value = null;
        if (line.length() != 0) {
            message.clearHeaders();
            do {
                char firstChar = line.charAt(0);
                if (name != null && (firstChar == ' ' || firstChar == '\t')) {
                    value = value + ' ' + line.trim();
                } else {
                    if (name != null) {
                        message.addHeader(name, value);
                    }
                    String[] header = splitHeader(line);
                    name = header[0];
                    value = header[1];
                }

                line = readHeader(buffer);
            } while (line.length() != 0);

            // Add the last header.
            if (name != null) {
                message.addHeader(name, value);
            }
        }

        State nextState;

        if (PagHeaders.getContentLength(message, -1) >= 0) {
            nextState = State.READ_FIXED_LENGTH_CONTENT;
        } else {
        	nextState = State.SKIP_CONTROL_CHARS;
        }
        return nextState;
    }

    private String readHeader(ChannelBuffer buffer) throws TooLongFrameException {
        StringBuilder sb = new StringBuilder(64);
        int headerSize = this.headerSize;

        loop:
        for (;;) {
            char nextByte = (char) buffer.readByte();
            headerSize ++;

            switch (nextByte) {
            case PagCodecUtil.CR:
                nextByte = (char) buffer.readByte();
                headerSize ++;
                if (nextByte == PagCodecUtil.LF) {
                    break loop;
                }
                break;
            case PagCodecUtil.LF:
                break loop;
            }

            // Abort decoding if the header part is too large.
            if (headerSize >= maxHeaderSize) {
                // TODO: Respond with Bad Request and discard the traffic
                //    or close the connection.
                //       No need to notify the upstream handlers - just log.
                //       If decoding a response, just throw an exception.
                throw new TooLongFrameException(
                        "HTTP header is larger than " +
                        maxHeaderSize + " bytes.");

            }

            sb.append(nextByte);
        }

        this.headerSize = headerSize;
        return sb.toString();
    }

    private PagMessage createMessage(String[] initialLine) {
    	if(PagMethod.valueOf(initialLine[0]) != null) {
    		// is Request
    		if(PagVersion.valueOf(initialLine[1]) != null) {
    			return new PagRequest(PagVersion.valueOf(initialLine[1]), PagMethod.valueOf(initialLine[0]));
    		}else if(PagVersion.valueOf(initialLine[2]) != null) {
    			return new PagRequest(PagVersion.valueOf(initialLine[2]), PagMethod.valueOf(initialLine[0]), initialLine[1]);    			
    		}
    	}else if(PagResponse.RESPONSE_METHOD.equalsIgnoreCase(initialLine[0])) {
    		// is Response
    		String ver = null;
    		String msg = null;
    		int sp_pos = findWhitespace(initialLine[2], 0);
    		if(sp_pos >= 0) {
	    		ver = initialLine[2].substring(0, sp_pos);
	    		msg = initialLine[2].substring(sp_pos+1);
    		}else {
    			ver = initialLine[2];
    		}
    		return new PagResponse(PagVersion.valueOf(ver), PagResponseStatus.valueOf(Integer.valueOf(initialLine[1]), msg));    			
    	}
    	return PagUnknownMessage.UNKNOWN;
    }

    private String readLine(ChannelBuffer buffer, int maxLineLength) throws TooLongFrameException {
        StringBuilder sb = new StringBuilder(64);
        int lineLength = 0;
        while (true) {
            byte nextByte = buffer.readByte();
            if (nextByte == PagCodecUtil.CR) {
                nextByte = buffer.readByte();
                if (nextByte == PagCodecUtil.LF) {
                    return sb.toString();
                }
            }
            else if (nextByte == PagCodecUtil.LF) {
                return sb.toString();
            }
            else {
                if (lineLength >= maxLineLength) {
                    // TODO: Respond with Bad Request and discard the traffic
                    //    or close the connection.
                    //       No need to notify the upstream handlers - just log.
                    //       If decoding a response, just throw an exception.
                    throw new TooLongFrameException(
                            "An HTTP line is larger than " + maxLineLength +
                            " bytes.");
                }
                lineLength ++;
                sb.append((char) nextByte);
            }
        }
    }

    private String[] splitInitialLine(String sb) {
        int aStart;
        int aEnd;
        int bStart;
        int bEnd;
        int cStart;
        int cEnd;

        aStart = findNonWhitespace(sb, 0);
        aEnd = findWhitespace(sb, aStart);

        bStart = findNonWhitespace(sb, aEnd);
        bEnd = findWhitespace(sb, bStart);

        cStart = findNonWhitespace(sb, bEnd);
        cEnd = findEndOfString(sb);

        return new String[] {
                sb.substring(aStart, aEnd),
                sb.substring(bStart, bEnd),
                cStart < cEnd? sb.substring(cStart, cEnd) : "" };
    }

    private String[] splitHeader(String sb) {
        final int length = sb.length();
        int nameStart;
        int nameEnd;
        int colonEnd;
        int valueStart;
        int valueEnd;

        nameStart = findNonWhitespace(sb, 0);
        for (nameEnd = nameStart; nameEnd < length; nameEnd ++) {
            char ch = sb.charAt(nameEnd);
            if (ch == ':' || Character.isWhitespace(ch)) {
                break;
            }
        }

        for (colonEnd = nameEnd; colonEnd < length; colonEnd ++) {
            if (sb.charAt(colonEnd) == ':') {
                colonEnd ++;
                break;
            }
        }

        valueStart = findNonWhitespace(sb, colonEnd);
        if (valueStart == length) {
            return new String[] {
                    sb.substring(nameStart, nameEnd),
                    ""
            };
        }

        valueEnd = findEndOfString(sb);
        return new String[] {
                sb.substring(nameStart, nameEnd),
                sb.substring(valueStart, valueEnd)
        };
    }

    private int findNonWhitespace(String sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result ++) {
            if (!Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    private int findWhitespace(String sb, int offset) {
        int result;
        for (result = offset; result < sb.length(); result ++) {
            if (Character.isWhitespace(sb.charAt(result))) {
                break;
            }
        }
        return result;
    }

    private int findEndOfString(String sb) {
        int result;
        for (result = sb.length(); result > 0; result --) {
            if (!Character.isWhitespace(sb.charAt(result - 1))) {
                break;
            }
        }
        return result;
    }
}
