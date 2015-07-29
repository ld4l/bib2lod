package org.ld4l.bib2lod.processor;

import java.io.File;

public class Processor {

    protected String localNamespace;
    protected String rdfFormat;
    
    protected File inputDir;
    protected File outputDir;
    protected File logDir;
    
    public Processor(String localNamespace, String rdfFormat, File inputDir, 
            File outputDir, File logDir) {
        
        this.localNamespace = localNamespace;
        this.rdfFormat = rdfFormat;
        
        this.inputDir = inputDir;
        this.logDir = logDir;
        this.outputDir = outputDir;
    }
    
    public void process(String[] actions) {
        System.out.println("Done processing!");
    }
    
}
