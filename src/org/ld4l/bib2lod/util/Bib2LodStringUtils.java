package org.ld4l.bib2lod.util;

public class Bib2LodStringUtils {

    private Bib2LodStringUtils() {}
    
    /**
     * Return singular or plural form of noun based on count. It's "simple" 
     * because it only handles plural forms in -s.
     * @param count The count of items
     * @param noun The string to pluralize
     * @return
     */
    
    public static final String plural(
            long count, String singular, String plural) {
        // 0 takes plural
        return (count == 1) ? singular : plural;
    }
    
    public static final String simplePlural(long count, String singular) {
        return plural(count, singular, singular + "s");
    }
    
    public static final String plural(
            int count, String singular, String plural) {
        return plural((long) count, singular, plural);
    }

    public static final String simplePlural(int count, String singular) {
        // 0 takes plural
        return simplePlural((long) count, singular);
    }
    
    public static final String count(
            long count, String singular, String plural) {
        return count + " " + plural(count, singular, plural);
    }
    
    public static final String count(long count, String singular) {
        return count(count, singular, singular + "s");
    }
}
