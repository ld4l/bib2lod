package org.ld4l.bib2lod.processor;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ProcessController {

    private static final Logger logger = LogManager.getLogger(ProcessController.class);
    
    protected String localNamespace;
    protected String rdfFormat;
    
    protected File inputDir;
    protected File outputDir;
    
    public ProcessController(String localNamespace, String rdfFormat, File inputDir, 
            File outputDir) {
        
        this.localNamespace = localNamespace;
        this.rdfFormat = rdfFormat;
        
        this.inputDir = inputDir;
        this.outputDir = outputDir;

    }
    
    public void process(List<String> actions) {
        logger.debug("Done!");
    }
}
