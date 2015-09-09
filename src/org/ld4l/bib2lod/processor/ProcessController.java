package org.ld4l.bib2lod.processor;

import java.util.List;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.Action;
import org.ld4l.bib2lod.Namespace;
import org.ld4l.bib2lod.processor.rdfconversion.BnodeConverter;
import org.ld4l.bib2lod.processor.rdfconversion.RdfCleaner;
import org.ld4l.bib2lod.processor.rdfconversion.TypeSplitter;
import org.ld4l.bib2lod.processor.rdfconversion.UriDeduper;



public class ProcessController {

    private static final Logger LOGGER = LogManager.getLogger(ProcessController.class);

    private String localNamespace;
    
    private String inputDir;
    private String mainOutputDir;
    
    private OntModel bfOntModelInf;
    // private OntModel ld4lInf;
    
    public ProcessController(String localNamespace, String inputDir, String outputDir) {
        
        this.localNamespace = localNamespace;
        
        this.inputDir = inputDir;
        this.mainOutputDir = outputDir;
        
        loadOntModels();
    }
    
    private void loadOntModels() {

        OntDocumentManager mgr = new OntDocumentManager();
        OntModelSpec spec = new OntModelSpec(OntModelSpec.OWL_MEM_MICRO_RULE_INF);
        spec.setDocumentManager(mgr);
     
        bfOntModelInf = ModelFactory.createOntologyModel(spec);
        // TODO Figure out how to avoid hard-coding the ontology URI here. It
        // would be nice to iterate through the files in the rdf directory and
        // read them in, but we need to retain a reference to the ontology.
        bfOntModelInf.read(Namespace.BIBFRAME.uri());
              
    }
    
    public String processAll(List<Action> selectedActions) {
        
        // As we move from one process to another, the output directory becomes
        // the input directory of the next process, and a new output directory
        // for the new process is created.
        String newInputDir = this.inputDir;
        String outputDir = newInputDir;
        
        // TODO Implement earlier actions: marcxml pre-processing, 
        // marcxml2bibframe conversion, etc.        
        // Correct errors in the Bibframe RDF that choke the ingest process.
        // Could do this after deduping, but probably these corrections should 
        // be included in deduped data.
        
        if (selectedActions.contains(Action.DEDUPE_URIS)) {

            // URI deduping requires some prior processing steps.
            
            outputDir = new RdfCleaner(localNamespace, outputDir, 
                    mainOutputDir).process();
            
            // Required since bnode ids are not guaranteed to be unique across
            // input files, so Jena may create duplicate ids across files when
            // reading into and writing out models. 
            outputDir = new BnodeConverter(localNamespace, 
                    outputDir, mainOutputDir).process();
            
            // Mechanism for handling large data files by reading only partial
            // data (split by type) into memory for deduping.
            outputDir = new TypeSplitter(bfOntModelInf,
                    localNamespace, outputDir, mainOutputDir).process();
            
            outputDir = new UriDeduper(bfOntModelInf,
                    localNamespace, outputDir, mainOutputDir).process();
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
                // LOGGER.trace("newInputDir = " + newInputDir);
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
                LOGGER.trace("STARTING PROCESS " + c.getName());
                LOGGER.trace("newInputDir = " + newInputDir);
                outputDir = processor.process();
                newInputDir = outputDir;
                LOGGER.trace("DONE WITH PROCESS " + c.getName());
                LOGGER.trace("Output dir = " + outputDir);                
            }
        }
        */
        
        // Return path to final results.
        // LOGGER.trace("Done with processAll()!");
        return outputDir;


        
    }


}
