package org.ld4l.bib2lod.util;


import java.time.Duration;
import java.time.Instant;

import org.junit.Assert;
import org.junit.Test;


public class UtilTest {

    @Test
    public void testDurationSecondsAsSeconds() {
        
        String expected = "01:01:06";
        int seconds = 3666;
        Instant start = Instant.now();        
        Instant end = start.plusSeconds(seconds);
        Duration duration = Duration.between(start, end);
        String actual = Util.formatSeconds(duration);
        Assert.assertEquals(expected, actual);       
    }

    @Test
    public void testDurationMillisAsSeconds() {
        
        String expected = "00:00:02";
        int millis = 2000;
        Instant start = Instant.now();        
        Instant end = start.plusMillis(millis);
        Duration duration = Duration.between(start, end);
        String actual = Util.formatSeconds(duration);
        Assert.assertEquals(expected, actual);       
    }

    @Test
    public void testDurationMillisAsMillis() {
        
        String expected = "00:00:02:222";
        int millis = 2222;
        Instant start = Instant.now();        
        Instant end = start.plusMillis(millis);
        Duration duration = Duration.between(start, end);
        String actual = Util.formatMillis(duration);
        Assert.assertEquals(expected, actual);       
    }
    
}
