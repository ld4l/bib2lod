package org.ld4l.bib2lod.util;

public final class Crc64 {

    private static final long POLY64REV = 0xd800000000000000L;

    private static final long[] LOOKUPTABLE;

    static {
        LOOKUPTABLE = new long[0x100];
        for (int i = 0; i < 0x100; i++) {
            long v = i;
            for (int j = 0; j < 8; j++) {
                if ((v & 1) == 1) {
                    v = (v >>> 1) ^ POLY64REV;
                } else {
                    v = (v >>> 1);
                }
            }
            LOOKUPTABLE[i] = v;
        }
    }
    
    /**
     * Calculates the Crc64 checksum for the given string.
     * @param s - the string to checksum
     * @return checksum value
     */
    public static long checksum(String s) {
        return checksum(s.getBytes());
    }

    /**
     * Calculates the Crc64 checksum for the given data array.
     * 
     * @param data - data to calculate checksum for
     *            
     * @return checksum value
     */
    public static long checksum(final byte[] data) {
        long sum = 0;
        for (final byte b : data) {
            final int lookupidx = ((int) sum ^ b) & 0xff;
            sum = (sum >>> 8) ^ LOOKUPTABLE[lookupidx];
        }
        return sum;
    }

    private Crc64() {
    }

}
