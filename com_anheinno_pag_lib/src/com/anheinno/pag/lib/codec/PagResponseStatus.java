package com.anheinno.pag.lib.codec;

public class PagResponseStatus implements Comparable<PagResponseStatus> {

    public static final PagResponseStatus OK = new PagResponseStatus(200, "OK");

    public static final PagResponseStatus BAD_REQUEST = new PagResponseStatus(400, "Bad Request");

    public static final PagResponseStatus UNAUTHORIZED = new PagResponseStatus(401, "Unauthorized");

    public static final PagResponseStatus FORBIDDEN = new PagResponseStatus(403, "Forbidden");

    public static final PagResponseStatus NOT_FOUND = new PagResponseStatus(404, "Not Found");

    public static final PagResponseStatus METHOD_NOT_ALLOWED = new PagResponseStatus(405, "Method Not Allowed");

    public static final PagResponseStatus NOT_ACCEPTABLE = new PagResponseStatus(406, "Not Acceptable");

    public static final PagResponseStatus REQUEST_TIMEOUT = new PagResponseStatus(408, "Request Timeout");

    public static final PagResponseStatus LENGTH_REQUIRED = new PagResponseStatus(411, "Length Required");

    public static final PagResponseStatus REQUEST_ENTITY_TOO_LARGE = new PagResponseStatus(413, "Request Entity Too Large");

    public static final PagResponseStatus VERSION_MISMATCH = new PagResponseStatus(414, "Version mismatch");

    public static final PagResponseStatus INTERNAL_SERVER_ERROR = new PagResponseStatus(500, "Internal Server Error");

    public static final PagResponseStatus NOT_IMPLEMENTED = new PagResponseStatus(501, "Not Implemented");

    public static final PagResponseStatus BAD_GATEWAY = new PagResponseStatus(502, "Bad Gateway");

    public static final PagResponseStatus PAG_VERSION_NOT_SUPPORTED = new PagResponseStatus(505, "Pag Version Not Supported");

    public static PagResponseStatus valueOf(int code, String reasonPhrase) {
        switch (code) {
        case 200:
            return OK;
        case 400:
            return BAD_REQUEST;
        case 401:
            return UNAUTHORIZED;
        case 403:
            return FORBIDDEN;
        case 404:
            return NOT_FOUND;
        case 405:
            return METHOD_NOT_ALLOWED;
        case 406:
            return NOT_ACCEPTABLE;
        case 408:
            return REQUEST_TIMEOUT;
        case 411:
            return LENGTH_REQUIRED;
        case 413:
            return REQUEST_ENTITY_TOO_LARGE;
        case 414:
        	return VERSION_MISMATCH;
        case 500:
            return INTERNAL_SERVER_ERROR;
        case 501:
            return NOT_IMPLEMENTED;
        case 502:
            return BAD_GATEWAY;
        case 505:
            return PAG_VERSION_NOT_SUPPORTED;
        }

        if(reasonPhrase == null) {;
	        if (code < 100) {
	            reasonPhrase = "Unknown Status";
	        } else if (code < 200) {
	            reasonPhrase = "Informational";
	        } else if (code < 300) {
	            reasonPhrase = "Successful";
	        } else if (code < 400) {
	            reasonPhrase = "Redirection";
	        } else if (code < 500) {
	            reasonPhrase = "Client Error";
	        } else if (code < 600) {
	            reasonPhrase = "Server Error";
	        } else {
	            reasonPhrase = "Unknown Status";
	        }
        }

        return new PagResponseStatus(code, reasonPhrase);
    }

    private final int code;

    private final String reasonPhrase;

    private PagResponseStatus(int code, String reasonPhrase) {
        if (code < 0) {
            throw new IllegalArgumentException(
                    "code: " + code + " (expected: 0+)");
        }

        if (reasonPhrase == null) {
            throw new NullPointerException("reasonPhrase");
        }

        for (int i = 0; i < reasonPhrase.length(); i ++) {
            char c = reasonPhrase.charAt(i);
            // Check prohibited characters.
            switch (c) {
            case '\n': case '\r':
                throw new IllegalArgumentException(
                        "reasonPhrase contains one of the following prohibited characters: " +
                        "\\r\\n: " + reasonPhrase);
            }
        }

        this.code = code;
        this.reasonPhrase = reasonPhrase;
    }

    public int getCode() {
        return code;
    }

    public String getReasonPhrase() {
        return reasonPhrase;
    }

    @Override
    public int hashCode() {
        return getCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PagResponseStatus)) {
            return false;
        }

        return getCode() == ((PagResponseStatus) o).getCode();
    }

    public int compareTo(PagResponseStatus o) {
        return getCode() - o.getCode();
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder(reasonPhrase.length() + 5);
        buf.append(code);
        buf.append(' ');
        buf.append(reasonPhrase);
        return buf.toString();
    }
}
