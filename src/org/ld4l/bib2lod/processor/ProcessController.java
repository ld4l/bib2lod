package org.ld4l.bib2lod.processor;

import java.lang.reflect.Constructor;
import java.util.List;

import org.apache.jena.ontology.OntDocumentManager;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.Action;
import org.ld4l.bib2lod.Namespace;



public class ProcessController {

    private static final Logger LOGGER = LogManager.getLogger(ProcessController.class);

    private String localNamespace;
    
    private String mainInputDir;
    private String mainOutputDir;
    
    private OntModel bfOntModelInf;
    // private OntModel ld4lInf;
    
    public ProcessController(String localNamespace, String inputDir, String outputDir) {
        
        this.localNamespace = localNamespace;
        
        this.mainInputDir = inputDir;
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
        String newInputDir = this.mainInputDir;
        String outputDir = null;
        
        // LOGGER.trace("STARTING processAll() method");
        // LOGGER.trace("mainInputDir = " + mainInputDir);
        // LOGGER.trace("newInputDir = " + newInputDir);

        // TODO Implement earlier actions: marcxml pre-processing, 
        // marcxml2bibframe conversion, etc.        
        // Correct errors in the Bibframe RDF that choke the ingest process.
        // Could do this after deduping, but probably these corrections should 
        // be included in deduped data.

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
                // LOGGER.trace("STARTING PROCESS " + c.getName());
                // LOGGER.trace("newInputDir = " + newInputDir);
                outputDir = processor.process();
                newInputDir = outputDir;
                // LOGGER.trace("DONE WITH PROCESS " + c.getName());
                // LOGGER.trace("Output dir = " + outputDir);                
            }
        }
        
        // Return path to final results.
        // LOGGER.trace("Done with processAll()!");
        return outputDir;

/* Previous approach where individual processors were called by name. 
        if (selectedActions.contains(Action.CONVERT_BNODES)) {
            
            Processor bnodeConverter = new BnodeConverter(localNamespace,
                    outputDir, mainOutputDir);
            outputDir = bnodeConverter.process();
        }
        if (selectedActions.contains(Action.DEDUPE_BIBFRAME_URIS)) {


            Processor splitter = new TypeSplitter(bfOntModelInf, 
                    localNamespace, outputDir, mainOutputDir);
            outputDir = splitter.process();
        }
        
        // TODO Insert other processes here
        // currentInputDir = previous outputDir
        // New outputDir defined by processor.
        // Stages:
        // Convert Bibframe RDF to LD4L RDF
        // Entity resolution
*/
        
    }


}
