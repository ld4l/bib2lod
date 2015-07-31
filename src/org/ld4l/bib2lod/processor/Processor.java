package org.ld4l.bib2lod.processor;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Processor {

    private static final Logger logger = LogManager.getLogger(Processor.class);
    
    protected String localNamespace;
    protected String rdfFormat;
    
    protected File inputDir;
    protected File outputDir;
  
}
