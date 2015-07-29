package org.ld4l.bib2lod.processor;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Processor {

    private static final Logger logger = LogManager.getLogger(Processor.class);
    
    protected String localNamespace;
    protected String rdfFormat;
    
    protected File inputDir;
    protected File outputDir;
    
    public Processor(String localNamespace, String rdfFormat, File inputDir, 
            File outputDir) {
        
        this.localNamespace = localNamespace;
        this.rdfFormat = rdfFormat;
        
        this.inputDir = inputDir;
        this.outputDir = outputDir;
        
        configureLogger();
    }
    
    public void process(String[] actions) {
        logger.info("Done!");
    }
    
    /** 
     * Configure the logger for this processor.
     */
    protected void configureLogger() {
        
    }
    
}
