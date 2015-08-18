package org.ld4l.bib2lod.processor.filesplitter;

import java.io.File;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.Processor;

public class TypeSplitter extends Processor {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(TypeSplitter.class);
    private static final String outputDirName = "statementsBySubjectType";
    
    public TypeSplitter(String localNamespace, String rdfFormat, String inputPath,
            String outputPath) {
        super(localNamespace, rdfFormat, inputPath, outputPath);

    }
    
    @Override
    protected String getOutputDirName() {
        return outputDirName;
    }

    @Override
    public String process() {

        String outputAbsoluteDirName = createOutputDir();
        if (outputAbsoluteDirName == null) {
            return null;
        }
        
        
        return outputAbsoluteDirName;

    }

}
