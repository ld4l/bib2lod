package org.ld4l.bib2lod;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class Processor {
    
    private static final Logger LOGGER = LogManager.getLogger(Processor.class);  
        
    private String outputDir;
    private final String mainOutputDir;
    protected final String inputDir;

    public Processor(String inputDir, String mainOutputDir) {
            
        LOGGER.trace("In constructor for " + this.getClass().toString());            
        this.inputDir = inputDir;
        this.mainOutputDir = mainOutputDir;
        this.outputDir = createOutputDir();              
    }

    public abstract String process();

    protected String getOutputDir() {
        return outputDir;
    }

    /**
     * Create a subdirectory of main output directory for the output of this
     * process.
     * @return - the full output directory path
     */
    private String createOutputDir() {
        
        String outputDir = null;
        String outputSubdir = 
                StringUtils.substringAfterLast(this.getClass().getName(), "."); 
        try {
            outputDir = Files.createDirectory(Paths.get(mainOutputDir, 
                    outputSubdir)).toString();
        } catch (IOException e) {
            e.printStackTrace();
            // TODO Abort program here
        }
        
        return outputDir;
    }
    
    protected String getOutputFilename(String basename) {
        //return basename + "." +  getOutputFormat().extension(); 
        return basename + getOutputFormat().fullExtension();
    }

    protected abstract Format getOutputFormat();
    
    protected String stubProcess() {
        copyFiles(inputDir);
        return outputDir;
    }
    
    /**
     * Copy a directory of files to this Processor's output directory.
     * @param inputDir - location of the input files
     */
    protected String copyFiles(String inputDir) {
        return copyFiles(inputDir, outputDir);       
    }
    
    /**
     * Copy a directory of files to the specified output directory.
     * @param inputDir - location of the input files
     * @param outputDir - directory for output files
     */
    protected String copyFiles(String inputDir, String outputDir) { 
        
        for ( File file : new File(inputDir).listFiles() ) {
            copyFile(file, outputDir);
        }
        return outputDir;
    }
    
    /**
     * Copy a file to this Processor's output directory.
     * @param file - the input file
     */
    protected String copyFile(File file) {
        return copyFile(file, outputDir);
    }
 
    /**
     * Copy a directory of files to the specified output directory.
     * @param inputDir - the input file
     * @param outputDir - directory for output files
     */
    protected String copyFile(File file, String outputDir) {

        Path source = file.toPath();
        String sourceFileName = file.getName();
        Path target = new File(outputDir, sourceFileName).toPath();
        try {
            Files.copy(source, target, COPY_ATTRIBUTES);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return outputDir;
    }
    
    public static String getCatalog(String localNamespace) {
        String[] elements = localNamespace.split("/");
        return elements[elements.length-1];
    }

}
