package org.ld4l.bib2lod;

import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BibframeConverter;
import org.ld4l.bib2lod.rdfconversion.OntNamespace;
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
    
    private OntModel bfOntModelInf;
    // private OntModel ld4lInf;
    
    public ProcessController(String localNamespace, String inputDir, 
            String outputDir, boolean erase) {
        
        this.localNamespace = localNamespace;
        
        this.inputDir = inputDir;
        this.mainOutputDir = outputDir;
        
        this.erase = erase;
        
        loadOntModels();
    }
    
    // TODO So far these are not used. Why aren't we using this instead of the
    // enums for ontology types and properties? Might be a more streamlined
    // and clearer solution.
    private void loadOntModels() {

        OntDocumentManager mgr = new OntDocumentManager();
        OntModelSpec spec = 
                new OntModelSpec(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        spec.setDocumentManager(mgr);
     
        bfOntModelInf = ModelFactory.createOntologyModel(spec);
        // TODO Figure out how to avoid hard-coding the ontology URI here. It
        // would be nice to iterate through the files in the rdf directory and
        // read them in, but we need to retain a reference to the ontology.
        bfOntModelInf.read(OntNamespace.BIBFRAME.uri());
              
    }
    
    public String processAll(Set<Action> selectedActions) {

        Instant start = Instant.now();
        int fileCount = new File(this.inputDir).listFiles().length;
        String inputFiles = fileCount + " input " 
                + Bib2LodStringUtils.simplePlural("file", fileCount);
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
                             
            outputDir = new UriGenerator(bfOntModelInf, localNamespace, 
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
