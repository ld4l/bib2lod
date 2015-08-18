package org.ld4l.bib2lod.processor.filesplitter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.Processor;

public class TypeSplitter extends Processor {

    private static final Logger logger = LogManager.getLogger(TypeSplitter.class);
    private static final String outputSubdir = "statementsBySubjectType";
    
    public TypeSplitter(String localNamespace, String rdfFormat, String inputDir,
            String mainOutputDir) {
        
        super(localNamespace, rdfFormat, inputDir, mainOutputDir);

    }
    


    @Override
    public String process() {

        logger.debug("input directory = " + inputDir);
        
        String outputDir = createOutputDir(outputSubdir);
        if (outputDir == null) {
            return null;
        }
        
        // Iterate through files in input dir
        // Create a set of models - one per type
        // Read file into a model
        // for each triple, get its type and add the triple to the appropriate
        // model
        // at the end, write each model out to a file

        return outputDir;

    }

}
