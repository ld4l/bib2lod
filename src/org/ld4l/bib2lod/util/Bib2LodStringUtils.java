package org.ld4l.bib2lod.util;

public class Bib2LodStringUtils {

    private Bib2LodStringUtils() {}
    
    /**
     * Return singular or plural form of noun based on count. It's "simple" 
     * because it only handles plural forms in -s.
     * @param noun The string to pluralize
     * @param count The count of items
     * @return
     */
    public static final String simplePlural(String noun, long count) {
        // 0 takes plural
        return (count == 1) ? noun : noun + "s";
    }

    public static final String simplePlural(String noun, int count) {
        // 0 takes plural
        return simplePlural(noun, (long) count);
    }
    
    public static final String count(String noun, long count) {
        return count + " " + simplePlural(noun, count);
    }
}
