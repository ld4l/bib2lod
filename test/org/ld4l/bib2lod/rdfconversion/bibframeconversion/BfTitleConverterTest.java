package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.junit.Assert;
import org.junit.Test;

public class BfTitleConverterTest {

    @Test
    public void testTitleNormalization1() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice.";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }
    
    @Test
    public void testTitleNormalization2() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice. ";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }

    @Test
    public void testTitleNormalization3() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice...";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }
    
    @Test
    public void testTitleNormalization4() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice... ";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }

    @Test
    public void testTitleNormalization5() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice;";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }
    
    @Test
    public void testTitleNormalization6() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice;";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }
    
}
