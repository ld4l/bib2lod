package org.ld4l.bib2lod.util;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

public final class TimerUtils {

    private TimerUtils() {}
    
    // Time processing of this many files or other items (URIs, resources, 
    // etc.). Individual classes may choose to define their own limits.
    public static final int NUM_FILES_TO_TIME = 10000;
    public static final int NUM_ITEMS_TO_TIME = 10000;

    public static String formatSeconds(Instant start, Instant end) {
        return formatSeconds(Duration.between(start, end));
    }
    
    public static String formatSeconds(Duration duration) {
        return formatSeconds(duration.getSeconds());
    }
    
    public static String formatSeconds(int seconds) {
        return formatSeconds(Long.valueOf(seconds));
    }
    
    public static String formatSeconds(Long seconds) {

        long hours = TimeUnit.SECONDS.toHours(seconds);
        seconds -= TimeUnit.HOURS.toSeconds(hours);
        
        long minutes = TimeUnit.SECONDS.toMinutes(seconds);
        seconds -= TimeUnit.MINUTES.toSeconds(minutes);
       
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }
 
    public static String formatMillis(Instant start, Instant end) {
        return formatMillis(Duration.between(start, end));
    }
    
    public static String formatMillis(Duration duration) {
        return formatMillis(duration.toMillis());
    }

    public static String formatMillis(int millis) {
        return formatMillis(Long.valueOf(millis));
    }
    
    public static String formatMillis(Long millis) {

        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);

        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        millis -= TimeUnit.SECONDS.toMillis(seconds);
       
        return String.format(
                "%02d:%02d:%02d.%03d", hours, minutes, seconds, millis);
    }
    
    public static String getDuration(Instant start) {
        return "Duration: " + formatMillis(start, Instant.now()) + ".";
    }
    

    
}
