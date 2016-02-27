package org.ld4l.bib2lod;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BibframeConverter;
import org.ld4l.bib2lod.rdfconversion.RdfCleaner;
import org.ld4l.bib2lod.rdfconversion.UriGenerator;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;


public class ProcessController {

    private static final Logger LOGGER = 
            LogManager.getLogger(ProcessController.class);

    private String localNamespace;
    
    private String inputDir;
    private String mainOutputDir;
    
    boolean erase;
    
    // private OntModel bfOntModel;
    // private OntModel ld4lOntModel;
    
    public ProcessController(String localNamespace, String inputDir, 
            String outputDir, boolean erase) {
        
        this.localNamespace = localNamespace;
        
        this.inputDir = inputDir;
        this.mainOutputDir = outputDir;
        
        this.erase = erase;
        
        // loadOntModels();
    }
    

//    private void loadOntModels() {
//
//        bfOntModel = ModelFactory.createOntologyModel();
//        try {
//            bfOntModel.read(OntNamespace.BIBFRAME.uri());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }                     
//    }

    
    public String processAll(Set<Action> selectedActions) {

//        if (bfOntModel.isEmpty()) {
//            return null;
//        }
        
        Instant start = Instant.now();
        int fileCount = new File(this.inputDir).listFiles().length;
        String inputFiles = fileCount + " input " 
                + Bib2LodStringUtils.simplePlural(fileCount, "file");
        LOGGER.info("Start converting " + inputFiles + " in " + this.inputDir 
                + ".");           
   
        // As we move from one process to another, the output directory becomes
        // the input directory of the next process, and a new output directory
        // for the new process is created.
        String newInputDir = this.inputDir;
        String outputDir = newInputDir;
        
        // TODO Implement earlier actions: marcxml pre-processing, 
        // marcxml2bibframe conversion, etc. 
        
        if (selectedActions.contains(Action.CLEAN_RDF)) {
            outputDir = new RdfCleaner(
                    localNamespace, newInputDir, mainOutputDir).process();
            newInputDir = deleteLastInputDir(newInputDir, outputDir);           
        }
     
        if (selectedActions.contains(Action.DEDUPE_RESOURCES)) {
                             
            outputDir = new UriGenerator(localNamespace, 
                    newInputDir, mainOutputDir).process(); 
                    
            newInputDir = deleteLastInputDir(newInputDir, outputDir);            
        }
        
        if (selectedActions.contains(Action.CONVERT_BIBFRAME)) {

            outputDir = new BibframeConverter(localNamespace, newInputDir, 
                    mainOutputDir).process();
            newInputDir = deleteLastInputDir(newInputDir, outputDir);
        }
            
        LOGGER.info("END CONVERSION! Total duration to convert " + inputFiles 
                + ": " + TimerUtils.formatMillis(start, Instant.now())
                + ". Results in " + outputDir + ".");
        return outputDir;
    }
    
    private String deleteLastInputDir(String lastInputDir, String newInputDir) {
        
        if (erase) {
            if (! lastInputDir.equals(inputDir)) {
                LOGGER.debug("Attempting to delete last input directory " +
                        lastInputDir);
                try {
                    FileUtils.deleteDirectory(new File(lastInputDir));
                    LOGGER.debug("Deleted last input directory " +  lastInputDir 
                            + ".");
                } catch (IOException e) {
                    LOGGER.warn("System error: can't delete last input "
                            + "directory " + lastInputDir);
                }
            } else {
                LOGGER.debug("Not erasing last input directory because it " +
                        "is the main input directory.");
            }
        } else {
            LOGGER.debug("Not erasing last input directory due to commandline " 
                    + "value.");            
        }
        return newInputDir;
    }
}
