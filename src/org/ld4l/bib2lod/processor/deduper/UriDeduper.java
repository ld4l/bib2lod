package org.ld4l.bib2lod.processor.deduper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.Processor;

// May need to be abstract - we only instantiate PersonDeduper, WorkDeduper, etc.
public class UriDeduper extends Processor {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(UriDeduper.class);
    private static final String outputDirName = "deduped";
    
    public UriDeduper(String localNamespace, String rdfFormat, String inputPath,
            String outputPath) {
        super(localNamespace, rdfFormat, inputPath, outputPath);

    }

    public String process() {
        return null;
    }

    @Override
    protected String getOutputDirName() {
        return outputDirName;
    }

    public static String getOutputdirname() {
        return outputDirName;
    }

    
}