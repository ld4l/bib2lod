package org.ld4l.bib2lod.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.filesplitter.TypeSplitter;

public class ProcessController {

    private static final Logger logger = LogManager.getLogger(ProcessController.class);
    
    protected String localNamespace;
    protected String rdfFormat;
    
    protected String inputPath;
    protected String mainOutputPath;
    
    public ProcessController(String localNamespace, String rdfFormat, 
            String inputPath, String outputPath) {
        
        this.localNamespace = localNamespace;
        this.rdfFormat = rdfFormat;
        
        this.inputPath = inputPath;
        this.mainOutputPath = outputPath;

    }
    
    public String processAll(List<String> actions) {
        
        // TODO Implement earlier actions: marcxml pre-processing, 
        // marcxml2bibframe conversion, etc.

        // TODO This should only be done in the case of certain actions:
        // deduping, for example.
        TypeSplitter splitter = new TypeSplitter(localNamespace, rdfFormat, 
                inputPath, mainOutputPath);
        String splitPath = splitter.process();

        // TODO Insert other processes here
        
        // Return path to final results (splitPath is only temporary here).
        String results = splitPath;
        logger.trace("Done!");
        return results;

    }


}
