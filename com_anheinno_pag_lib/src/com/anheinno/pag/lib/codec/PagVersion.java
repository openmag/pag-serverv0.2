package com.anheinno.pag.lib.codec;

public class PagVersion implements Comparable<PagVersion> {

    //private static final Pattern VERSION_PATTERN =
    //    Pattern.compile("(\\S+)/(\\d+)\\.(\\d+)");

    public static final PagVersion PAM_1_0 = new PagVersion("PAM", 1, 0);
    public static final PagVersion PAA_1_0 = new PagVersion("PAA", 1, 0);

    public static PagVersion valueOf(String text) {
        if (text == null) {
            throw new NullPointerException("text");
        }

        text = text.trim().toUpperCase();
        if (text.equals("PAM/1.0")) {
            return PAM_1_0;
        }else if(text.equals("PAA/1.0")) {
        	return PAA_1_0;
        }
        
        return null; //new PagVersion(text);
    }

    private final String protocolName;
    private final int majorVersion;
    private final int minorVersion;
    private final String text;

    /*private PagVersion(String text) {
        if (text == null) {
            throw new NullPointerException("text");
        }

        if (text.length() == 0) {
            throw new IllegalArgumentException("empty text");
        }

        text = text.trim().toUpperCase();

        Matcher m = VERSION_PATTERN.matcher(text);
        if (!m.matches()) {
            throw new IllegalArgumentException("invalid version format: " + text);
        }

        protocolName = m.group(1);
        majorVersion = Integer.parseInt(m.group(2));
        minorVersion = Integer.parseInt(m.group(3));
        this.text = protocolName + '/' + majorVersion + '.' + minorVersion;
    }*/

    private PagVersion(String protocolName, int majorVersion, int minorVersion) {
        if (protocolName == null) {
            throw new NullPointerException("protocolName");
        }

        protocolName = protocolName.trim().toUpperCase();
        if (protocolName.length() == 0) {
            throw new IllegalArgumentException("empty protocolName");
        }

        for (int i = 0; i < protocolName.length(); i ++) {
            if (Character.isISOControl(protocolName.charAt(i)) ||
                Character.isWhitespace(protocolName.charAt(i))) {
                throw new IllegalArgumentException("invalid character in protocolName");
            }
        }

        if (majorVersion < 0) {
            throw new IllegalArgumentException("negative majorVersion");
        }
        if (minorVersion < 0) {
            throw new IllegalArgumentException("negative minorVersion");
        }

        this.protocolName = protocolName;
        this.majorVersion = majorVersion;
        this.minorVersion = minorVersion;
        text = protocolName + '/' + majorVersion + '.' + minorVersion;
    }

    /**
     * Returns the name of the protocol such as {@code "HTTP"} in {@code "HTTP/1.0"}.
     */
    public String getProtocolName() {
        return protocolName;
    }

    /**
     * Returns the name of the protocol such as {@code 1} in {@code "HTTP/1.0"}.
     */
    public int getMajorVersion() {
        return majorVersion;
    }

    /**
     * Returns the name of the protocol such as {@code 0} in {@code "HTTP/1.0"}.
     */
    public int getMinorVersion() {
        return minorVersion;
    }

    /**
     * Returns the full protocol version text such as {@code "HTTP/1.0"}.
     */
    public String getText() {
        return text;
    }

    /**
     * Returns the full protocol version text such as {@code "HTTP/1.0"}.
     */
    @Override
    public String toString() {
        return getText();
    }

    @Override
    public int hashCode() {
        return (getProtocolName().hashCode() * 31 + getMajorVersion()) * 31 +
               getMinorVersion();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PagVersion)) {
            return false;
        }

        PagVersion that = (PagVersion) o;
        return getMinorVersion() == that.getMinorVersion() &&
               getMajorVersion() == that.getMajorVersion() &&
               getProtocolName().equals(that.getProtocolName());
    }

    public int compareTo(PagVersion o) {
        int v = getProtocolName().compareTo(o.getProtocolName());
        if (v != 0) {
            return v;
        }

        v = getMajorVersion() - o.getMajorVersion();
        if (v != 0) {
            return v;
        }

        return getMinorVersion() - o.getMinorVersion();
    }
}
