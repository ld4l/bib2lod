package org.ld4l.bib2lod.util;

public final class Crc64Mod {

    private static final long POLY64REV = 0xd800000000000000L;
    private static final long[] CRC_TABLE;
    
    static {
        CRC_TABLE = new long[0x100];
        for (int i = 0; i < 0x100; i++) {
            long v = i;

             for (int j = 0; j < 8; j++) {
                  long newV = v >>> 1;
                  
                  if ((v & 0x100000000L) != 0) {
                     newV |= 0x100000000L;
                  }
                  
                  if ((v & 1) != 0) {
                      newV ^= POLY64REV;
                 }
                  
                  v = newV;
              }
            
            CRC_TABLE[i] = v;
        }       
    }
    
    public static long checksum(String s) {
        return checksum(s.getBytes());
    }
    
    public static long checksum(final byte[] bytes) {
        
        long sum = 0;
        
        for (int i = 0; i < bytes.length; i++) {
            final int crcIndex = ((int) sum ^ bytes[i]) & 0xff;   
            sum = (sum >>> 8) ^ CRC_TABLE[crcIndex];
        }

        return sum;
    }

    
    private Crc64Mod() { }
    
}