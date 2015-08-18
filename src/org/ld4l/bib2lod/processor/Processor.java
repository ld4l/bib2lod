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
    
    protected String inputPath;
    private String mainOutputPath;
    
    
    public Processor(String localNamespace, String rdfFormat, String inputPath, 
            String outputPath) {
        
        this.localNamespace = localNamespace;
        this.rdfFormat = rdfFormat;
        
        this.inputPath = inputPath;
        this.mainOutputPath = outputPath;

    }


    // TODO Not sure whether getters should be public or protected
    public String getLocalNamespace() {
        return localNamespace;
    }

    // TODO Not sure whether getters should be public or protected
    public String getRdfFormat() {
        return rdfFormat;
    }

    // TODO Not sure whether getters should be public or protected
    public String getInputPath() {
        return inputPath;
    }

    public String getMainOutputPath() {
        return mainOutputPath;
    }

    public abstract String process();
    
    
    protected String createOutputDir() {
        
        String outputDirName = null;
        
        try {
            outputDirName = Files.createDirectory(Paths.get(mainOutputPath, 
                    getOutputDirName())).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return outputDirName;
    }
    
    protected abstract String getOutputDirName();

  
}
