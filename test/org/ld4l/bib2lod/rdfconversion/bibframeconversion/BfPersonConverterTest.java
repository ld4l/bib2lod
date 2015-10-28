package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Map;

import org.junit.Assert;
import org.junit.Test;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfPersonConverterTest {

    @Test
    public void testNameParsing1() {
        
        String name = "Gordon, Burgess L. (Burgess Lee)";
        String label = "Gordon, Burgess L. (Burgess Lee)";
        BfPersonConverter converter = getConverter();
        Map<OntProperty, String> parsedLabel = converter.parseLabel(label);
        Assert.assertEquals(name, parsedLabel.get(OntProperty.NAME));
        
    }
    
    @Test
    public void testNameParsing2() {
        
        String name = "Gordon, Burgess L. (Burgess Lee)";
        String label = "Gordon, Burgess L. (Burgess Lee), 1892-";
        BfPersonConverter converter = getConverter();
        Map<OntProperty, String> parsedLabel = converter.parseLabel(label);
        Assert.assertEquals(name, parsedLabel.get(OntProperty.NAME));
        
    }
    
    @Test
    public void testNameParsing3() {
        
        String name = "Gordon, Burgess L. (Burgess Lee)";
        String label = "Gordon, Burgess L. (Burgess Lee), 1892-1935";
        BfPersonConverter converter = getConverter();
        Map<OntProperty, String> parsedLabel = converter.parseLabel(label);
        Assert.assertEquals(name, parsedLabel.get(OntProperty.NAME));
        
    }
    
    @Test
    public void testNameParsing4() {
        
        String name = "Gordon, Burgess L. (Burgess Lee)";
        String label = "Gordon, Burgess L. (Burgess Lee), -1935";
        BfPersonConverter converter = getConverter();
        Map<OntProperty, String> parsedLabel = converter.parseLabel(label);
        Assert.assertEquals(name, parsedLabel.get(OntProperty.NAME));
        
    }

    private BfPersonConverter getConverter() {
        return new BfPersonConverter(OntType.BF_PERSON);
        
    }
}
