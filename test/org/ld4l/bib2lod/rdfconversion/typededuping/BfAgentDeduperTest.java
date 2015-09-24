package org.ld4l.bib2lod.rdfconversion.typededuping;

import org.junit.Assert;
import org.junit.Test;
import org.ld4l.bib2lod.rdfconversion.typededuping.BfAgentDeduper;

public class BfAgentDeduperTest {
    
    private BfAgentDeduper deduper = new BfAgentDeduper();

    @Test
    public void testNormalizeAuthorityName1() {
        
        String normalizedName = "twain,_mark_1835_1910";        
        String name = "Twain, Mark, 1835-1910";
        name = deduper.normalizeAuthorityName(name);
        Assert.assertEquals(name, normalizedName);       
    }

    @Test
    public void testNormalizeAuthorityName2() {

        String normalizedName = "twain,_mark_1835_1910";        
        String name = "Twain, Mark, 1835-1910.";
        name = deduper.normalizeAuthorityName(name);
        Assert.assertEquals(name, normalizedName);        
    }
    
    @Test
    public void testNormalizeAuthorityName3() {

      String normalizedName = "gordon,_burgess_l_burgess_lee_1892";      
      String name = "Gordon, Burgess L. (Burgess Lee), 1892-";
      name = deduper.normalizeAuthorityName(name);
      Assert.assertEquals(name, normalizedName);      
    }

    
    @Test
    public void testNormalizeAuthorityName4() {

        String normalizedName = "railton,_stephen_1948";
        String name = "Railton, Stephen, 1948-"; 
        name = deduper.normalizeAuthorityName(name);
        Assert.assertEquals(name, normalizedName);        
    }
    
    @Test
    public void testNormalizeAuthorityName5() {

        String normalizedName = "bancroft_library_mark_twain_project";
        String name = "Bancroft Library. Mark Twain Project."; 
        name = deduper.normalizeAuthorityName(name);
        Assert.assertEquals(name, normalizedName);         
    }
    
    @Test
    public void testNormalizeAuthorityName6() {


        
    }
    
}
