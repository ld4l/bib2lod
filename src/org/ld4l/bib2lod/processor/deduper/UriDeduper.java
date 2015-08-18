package org.ld4l.bib2lod.processor.deduper;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.RdfFormat;
import org.ld4l.bib2lod.processor.Processor;
import org.ld4l.bib2lod.processor.filesplitter.TypeSplitter;

// May need to be abstract - we only instantiate PersonDeduper, WorkDeduper, etc.
public class UriDeduper extends Processor {

    private static final Logger logger = LogManager.getLogger(TypeSplitter.class);
    private static final String outputSubdir = "deduped";
    
    public UriDeduper(String localNamespace, RdfFormat rdfFormat, String inputDir,
            String mainOutputDir) {
        
        super(localNamespace, rdfFormat, inputDir, mainOutputDir);

    }
    


    @Override
    public String process() {

        String outputDir = createOutputDir(outputSubdir);
        if (outputDir == null) {
            return null;
        }
        
        logger.debug("input directory = " + inputDir);
        
        return outputDir;

    }

}