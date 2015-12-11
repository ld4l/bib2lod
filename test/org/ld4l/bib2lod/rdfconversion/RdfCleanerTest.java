package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class RdfCleanerTest {
    
    RdfCleaner cleaner;
    File testData;
    
    @Before
    public void initialize() {
        testData = new File("test-data");
        File testInDir = new File("test-data/in");
        File testOutDir = new File("test-data/out");
        testInDir.mkdirs();
        testOutDir.mkdir();
        String testIn = testInDir.getAbsolutePath();
        String testOut = testOutDir.getAbsolutePath();
        System.out.println(testIn);
        System.out.println(testOut);

        String localNamespace = "http://draft.ld4l.org/cornell/";
        
        cleaner = new RdfCleaner(
                localNamespace, testIn, testOut);
    }
    
    @After
    public void cleanup() {
        try {
            FileUtils.deleteDirectory(testData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testFixLocalName() {

        String expected = "<http://draft.ld4l.org/cornell/n120632> "
                          + "<http://bibframe.org/vocab/hasInstance> "
                          + "<http://draft.ld4l.org/cornell/n120632instance17";
        
        String line = "<http://draft.ld4l.org/cornell/120632> "
                      + "<http://bibframe.org/vocab/hasInstance> "
                      + "<http://draft.ld4l.org/cornell/120632instance17";
   
        line = cleaner.fixLocalNames(line);
        
        Assert.assertEquals(expected, line);
        
    }
    


}
