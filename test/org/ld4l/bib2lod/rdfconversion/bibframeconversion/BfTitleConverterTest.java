package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.junit.Assert;
import org.junit.Test;

public class BfTitleConverterTest {

    @Test
    public void testTitleNormalizationPeriod() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice.";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }

    @Test
    public void testTitleNormalizationSpace() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice ";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }
       
    @Test
    public void testTitleNormalizationPeriodSpace() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice. ";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }

    @Test
    public void testTitleNormalizationSpacePeriodSpace() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice . ";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }
    
    @Test
    public void testTitleNormalizationEllipsis() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice...";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }
    
    @Test
    public void testTitleNormalizationEllipsisSpace() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice... ";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }


    @Test
    public void testTitleNormalizationSpaceEllipsis() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice ...";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }

    @Test
    public void testTitleNormalizationSpaceEllipsisSpace() {
        
        String expected = "Pride and Prejudice";
        String input = "Pride and Prejudice ... ";
        Assert.assertEquals(expected, BfTitleConverter.normalizeTitle(input));        
    }
    
}
