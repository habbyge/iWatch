package com.habbyge.iwatch.util;

import java.math.BigInteger;

public final class Util {
    private Util() {
    }

    public static String long2UnsignedString(long seq) {
        String bin = Long.toBinaryString(seq);
        BigInteger big = new BigInteger(bin, 2);
        return big.toString();
    }

    public long unsignedString2Long(String seq) {
        if(seq == null || seq.isEmpty()){
            return 0L;
        }
        return new BigInteger(seq).longValue();
    }
}
