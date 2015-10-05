package org.ld4l.bib2lod.rdfconversion.naco;

import org.junit.Assert;
import org.junit.Test;
import org.ld4l.bib2lod.rdfconversion.typededuping.BfAgentDeduper;

// TODO Write more tests
public class NacoNormalizerTest {

    @Test
    public void testNormalize1() {
        
        String normalizedName = "TWAIN, MARK 1835 1910";        
        String name = "Twain, Mark, 1835-1910";
        name = NacoNormalizer.normalize(name);
        Assert.assertEquals(name, normalizedName);       
    }

    @Test
    public void testNormalize2() {

        String normalizedName = "TWAIN, MARK 1835 1910";        
        String name = "Twain, Mark, 1835-1910.";
        name = NacoNormalizer.normalize(name);
        Assert.assertEquals(name, normalizedName);        
    }
    
    @Test
    public void testNormalize3() {

      String normalizedName = "GORDON, BURGESS L BURGESS LEE 1892";      
      String name = "Gordon, Burgess L. (Burgess Lee), 1892-";
      name = NacoNormalizer.normalize(name);
      Assert.assertEquals(name, normalizedName);      
    }

    
    @Test
    public void testNormalize4() {

        String normalizedName = "RAILTON, STEPHEN 1948";
        String name = "Railton, Stephen, 1948-"; 
        name = NacoNormalizer.normalize(name);
        Assert.assertEquals(name, normalizedName);        
    }
    
    @Test
    public void testNormalize5() {

        String normalizedName = "BANCROFT LIBRARY MARK TWAIN PROJECT";
        String name = "Bancroft Library. Mark Twain Project."; 
        name = NacoNormalizer.normalize(name);
        Assert.assertEquals(name, normalizedName);         
    }
    
    @Test
    public void testNormalizeNull() {

        String s = NacoNormalizer.normalize(null);
        Assert.assertNull(s);                
    }
    
}
