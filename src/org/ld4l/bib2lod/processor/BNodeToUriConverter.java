package org.ld4l.bib2lod.processor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BNodeToUriConverter extends Processor {

    @SuppressWarnings("unused")
    private static final Logger logger = 
            LogManager.getLogger(BNodeToUriConverter.class);
    private static final String outputSubdir = "noBNodes";
    
    public BNodeToUriConverter(String localNamespace, 
            String inputDir, String mainOutputDir) {
                        
        super(localNamespace, inputDir, mainOutputDir);

    }

    @Override
    public String process() {
        
        return stubProcess(outputSubdir);
        
    }
}
