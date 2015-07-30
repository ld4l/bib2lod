package org.ld4l.bib2lod.processor.deduper;

import java.io.File;
import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.Processor;


public class Deduper extends Processor {
    
    private static final Logger logger = LogManager.getLogger(Deduper.class);

    public Deduper(String localNamespace, String rdfFormat, File inputDir, 
            File outputDir) {
        super(localNamespace, rdfFormat, inputDir, outputDir);
        // TODO Auto-generated constructor stub
    }
    
    @Override
    protected void configureLogger() {
        
    }

    @Override
    public void process(List<String> actions) {
        logger.info("Done!");
    }
    
}