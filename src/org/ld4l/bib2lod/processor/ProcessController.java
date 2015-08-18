package org.ld4l.bib2lod.processor;

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.filesplitter.TypeSplitter;

public class ProcessController {

    private static final Logger logger = LogManager.getLogger(ProcessController.class);
    
    protected String localNamespace;
    protected String rdfFormat;
    
    protected String inputDir;
    protected String mainOutputDir;
    
    public ProcessController(String localNamespace, String rdfFormat, 
            String inputDir, String outputDir) {
        
        this.localNamespace = localNamespace;
        this.rdfFormat = rdfFormat;
        
        this.inputDir = inputDir;
        this.mainOutputDir = outputDir;

    }
    
    public String processAll(List<String> actions) {
        
        // As we move from one process to another, the output directory becomes
        // the input directory of the next process, and a new output directory
        // for the new process is created.
        String outputDir = null;
        String currentInputDir = this.inputDir;
        
        // TODO Implement earlier actions: marcxml pre-processing, 
        // marcxml2bibframe conversion, etc.

        // NB If there are earlier actions, the TypeSplitter constructor gets
        // passed in the resultsDir of the previous process, not this.inputPath.
        if (actions.contains("dedupe")) {
            TypeSplitter splitter = new TypeSplitter(localNamespace, rdfFormat, 
                    currentInputDir, mainOutputDir);
            outputDir = splitter.process();
        }
        
        // TODO Insert other processes here
        // outputDir becomes new currentInputDir

        // Return path to final results.
        logger.trace("Done!");
        return outputDir;

    }


}
