package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

public class BfPersonConverterTest {

    @Test
    public void testNameParsing1() {
        
        String name = "Gordon, Burgess L. (Burgess Lee)";
        String label = "Gordon, Burgess L. (Burgess Lee)";
        Map<Ld4lProperty, String> parsedLabel = 
                BfPersonConverter.parseLabel(label);
        Assert.assertEquals(name, parsedLabel.get(Ld4lProperty.NAME));
        
    }
    
    @Test
    public void testNameParsing2() {
        
        String name = "Gordon, Burgess L. (Burgess Lee)";
        String label = "Gordon, Burgess L. (Burgess Lee), 1892-";
        Map<Ld4lProperty, String> parsedLabel = 
                BfPersonConverter.parseLabel(label);
        Assert.assertEquals(name, parsedLabel.get(Ld4lProperty.NAME));
        
    }
    
    @Test
    public void testNameParsing3() {
        
        String name = "Gordon, Burgess L. (Burgess Lee)";
        String label = "Gordon, Burgess L. (Burgess Lee), 1892-1935";
        Map<Ld4lProperty, String> parsedLabel = 
                BfPersonConverter.parseLabel(label);
        Assert.assertEquals(name, parsedLabel.get(Ld4lProperty.NAME));
        
    }
    
    @Test
    public void testNameParsing4() {
        
        String name = "Gordon, Burgess L. (Burgess Lee)";
        String label = "Gordon, Burgess L. (Burgess Lee), -1935";
        Map<Ld4lProperty, String> parsedLabel = 
                BfPersonConverter.parseLabel(label);
        Assert.assertEquals(name, parsedLabel.get(Ld4lProperty.NAME));
        
    }
}
