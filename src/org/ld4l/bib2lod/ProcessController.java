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
import org.ld4l.bib2lod.rdfconversion.BnodeConverter;
import org.ld4l.bib2lod.rdfconversion.OntNamespace;
import org.ld4l.bib2lod.rdfconversion.RdfCleaner;
import org.ld4l.bib2lod.rdfconversion.ResourceDeduper;
import org.ld4l.bib2lod.rdfconversion.TypeSplitter;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;



public class ProcessController {

    private static final Logger LOGGER = LogManager.getLogger(ProcessController.class);

    private String localNamespace;
    
    private String inputDir;
    private String mainOutputDir;
    
    private OntModel bfOntModelInf;
    // private OntModel ld4lInf;
    
    public ProcessController(String localNamespace, String inputDir, 
            String outputDir) {
        
        this.localNamespace = localNamespace;
        
        this.inputDir = inputDir;
        this.mainOutputDir = outputDir;
        
        loadOntModels();
    }
    
    // TODO So far these are not used. Why aren't we using this instead of the
    // enums for ontology types and properties? That might be a more stream-
    // lined solution.
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
            LOGGER.debug("newInputDir before RDF cleanup: " + newInputDir);
            outputDir = new RdfCleaner(
                    localNamespace, newInputDir, mainOutputDir).process();
            newInputDir = deleteLastInputDir(newInputDir, outputDir);
            LOGGER.debug("newInputDir after RDF cleanup: " + newInputDir);            
        }
     
        if (selectedActions.contains(Action.DEDUPE_RESOURCES)) {

            // Resource deduping requires some prior processing steps.
            
            // This is now an independent action, so is handled above.
            // outputDir = new RdfCleaner(
            //        localNamespace, outputDir, mainOutputDir).process();
                             
            /* Required since bnode ids are not guaranteed to be unique across
             * input files, so Jena may create duplicate ids across files when
             * reading into and writing out models. 
             * NB Not including in RdfCleaner as a string replacement, since
             * rdf/xml input doesn't contain explicit bnodes.
             */
            LOGGER.debug("newInputDir before bnode conversion: " + newInputDir);
            outputDir = new BnodeConverter(
                    localNamespace, newInputDir, mainOutputDir).process(); 
            newInputDir = deleteLastInputDir(newInputDir, outputDir);
            LOGGER.debug("newInputDir before bnode conversion: " + newInputDir);
            
            // Strategy for handling large data files by reading only partial
            // data (split by type) into memory for deduping.
            LOGGER.debug("newInputDir before type-splitting: " + newInputDir);
            outputDir = new TypeSplitter(newInputDir, mainOutputDir).process();
            newInputDir = deleteLastInputDir(newInputDir, outputDir);
            LOGGER.debug("newInputDir after type-splitting: " + newInputDir);

            LOGGER.debug("newInputDir before URI deduping: " + newInputDir);
            outputDir = new ResourceDeduper(
                    newInputDir, mainOutputDir).process();
            newInputDir = deleteLastInputDir(newInputDir, outputDir);
            LOGGER.debug("newInputDir after URI deduping: " + newInputDir);
        }
        
        if (selectedActions.contains(Action.CONVERT_BIBFRAME)) {
            LOGGER.debug("newInputDir before Bibframe conversion: " 
                    + newInputDir);
            outputDir = new BibframeConverter(localNamespace, newInputDir, 
                    mainOutputDir).process();
            newInputDir = deleteLastInputDir(newInputDir, outputDir);
            LOGGER.debug("newInputDir after Bibframe conversion: " 
                    + newInputDir);
        }
            
        /* Previous approach where processors were called by looping through
         * Action values. Switch to calling processors by name in order to
         * express dependencies. Dependencies could be automated by defining
         * them in the Action enum, but for now this is unnecessarily 
         * complex. 
         *
        // Loop on defined Actions rather than selected Actions to ensure 
        // correct order.
        for (Action a : Action.values()) {
            Class<?> c = a.processorClass();
            if (selectedActions.contains(a)) {
                Processor processor;
                Constructor<?> constructor; 
                try {
                    constructor = c.getConstructor(
                            OntModel.class, String.class, String.class, 
                            String.class);
                    processor = (Processor) constructor.newInstance(
                            bfOntModelInf,localNamespace, newInputDir, 
                            mainOutputDir);
                } catch (NoSuchMethodException e) {
                    try {
                        constructor = c.getConstructor(String.class, 
                                String.class, String.class);
                        processor = (Processor) constructor.newInstance(
                                localNamespace, newInputDir, mainOutputDir);          
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                outputDir = processor.process();
                newInputDir = outputDir;              
            }
        }
        */

        LOGGER.info("END CONVERSION! Total duration to convert " + inputFiles 
                + ": " + TimerUtils.formatMillis(start, Instant.now())
                + ". Results in " + outputDir + ".");
        return outputDir;
 
    }
    
    private String deleteLastInputDir(String lastInputDir, String newInputDir) {
        if (! lastInputDir.equals(inputDir)) {
            try {
                FileUtils.deleteDirectory(new File(lastInputDir));
                LOGGER.debug("Deleted last input directory " +  lastInputDir 
                        + ".");
            } catch (IOException e) {
                LOGGER.warn("Can't delete last input directory " 
                        + lastInputDir);
            }
        }
        return newInputDir;
    }
}
