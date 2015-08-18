package org.ld4l.bib2lod.processor.filesplitter;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.RdfFormat;
import org.ld4l.bib2lod.processor.Processor;

public class TypeSplitter extends Processor {

    private static final Logger logger = LogManager.getLogger(TypeSplitter.class);
    private static final String outputSubdir = "statementsBySubjectType";
    
    public TypeSplitter(String localNamespace, RdfFormat rdfFormat, String inputDir,
            String mainOutputDir) {
        
        super(localNamespace, rdfFormat, inputDir, mainOutputDir);

    }
    


    @Override
    public String process() {

        String outputDir = createOutputDir(outputSubdir);
        if (outputDir == null) {
            return null;
        }

        // Create a set of models - one per type
        // Iterate through files in input dir
        // Read file into a model
        // For each type: get all the statements with subjects of that type
        // write to the appropriate model
        // at the end, write each output model to a file

        return outputDir;

    }

}
