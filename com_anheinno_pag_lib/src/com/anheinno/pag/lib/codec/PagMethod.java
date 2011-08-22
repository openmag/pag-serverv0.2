package com.anheinno.pag.lib.codec;

import java.util.HashMap;
import java.util.Map;

public class PagMethod implements Comparable<PagMethod> {

    public static final PagMethod REG = new PagMethod("REG");

    public static final PagMethod ACT = new PagMethod("ACT");

    public static final PagMethod NOTI = new PagMethod("NOTI");

    //public static final PagMethod POST = new PagMethod("POST");

    public static final PagMethod ACK = new PagMethod("ACK");

    public static final PagMethod STAT = new PagMethod("STAT");

    public static final PagMethod BYE = new PagMethod("BYE");

    private static final Map<String, PagMethod> methodMap =
            new HashMap<String, PagMethod>();

    static {
        methodMap.put(REG.toString(),  REG);
        methodMap.put(ACT.toString(),  ACT);
        methodMap.put(NOTI.toString(), NOTI);
        //methodMap.put(POST.toString(), POST);
        methodMap.put(STAT.toString(), STAT);
        methodMap.put(ACK.toString(),  ACK);
        methodMap.put(BYE.toString(),  BYE);
    }

    public static PagMethod valueOf(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        name = name.trim().toUpperCase();
        if (name.length() == 0) {
            throw new IllegalArgumentException("empty name");
        }

        PagMethod result = methodMap.get(name);
        if (result != null) {
            return result;
        } else {
            return null; //new PagMethod(name);
        }
    }

    private final String name;

    private PagMethod(String name) {
        if (name == null) {
            throw new NullPointerException("name");
        }

        name = name.trim().toUpperCase();
        if (name.length() == 0) {
            throw new IllegalArgumentException("empty name");
        }

        for (int i = 0; i < name.length(); i ++) {
            if (Character.isISOControl(name.charAt(i)) ||
                Character.isWhitespace(name.charAt(i))) {
                throw new IllegalArgumentException("invalid character in name");
            }
        }

        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public int hashCode() {
        return getName().hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof PagMethod)) {
            return false;
        }

        PagMethod that = (PagMethod) o;
        return getName().equals(that.getName());
    }

    @Override
    public String toString() {
        return getName();
    }

    public int compareTo(PagMethod o) {
        return getName().compareTo(o.getName());
    }
}