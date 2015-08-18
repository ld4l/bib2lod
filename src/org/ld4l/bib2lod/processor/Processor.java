package org.ld4l.bib2lod.processor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Processor {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(Processor.class);
    
    protected String localNamespace;
    protected String rdfFormat;
    
    protected String inputDir;
    private String mainOutputDir;
    
    
    public Processor(String localNamespace, String rdfFormat, String inputDir, 
            String mainOutputDir) {
        
        this.localNamespace = localNamespace;
        this.rdfFormat = rdfFormat;
        
        this.inputDir = inputDir;
        this.mainOutputDir = mainOutputDir;

    }

    
    public abstract String process();
    
    
    /**
     * Create a subdirectory of main output directory for the output of this
     * process.
     * @param subDirName - the output subdirectory name for this process
     * @return - the full output directory path
     */
    protected String createOutputDir(String subdir) {
        
        String outputDir = null;
        
        try {
            outputDir = Files.createDirectory(Paths.get(mainOutputDir, 
                    subdir)).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return outputDir;
    }
    

  
}
