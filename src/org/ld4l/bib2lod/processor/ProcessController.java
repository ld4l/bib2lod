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



public class ProcessController {

    private static final Logger logger = LogManager.getLogger(ProcessController.class);

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
        bfOntModelInf.read("http://bibframe.org/vocab/");
              
//        if (logger.isDebugEnabled()) {
//            Model m = bfOntModelInf.getBaseModel();
//            //logger.debug(bfOntModelInf.toString());
//            //logger.debug(m.toString());
//            logger.debug(m.size());
//            logger.debug(bfOntModelInf.size());
//            Iterator<OntClass> it = bfOntModelInf.listClasses();
//            while (it.hasNext()) {
//                OntClass c = it.next();
//                logger.debug(c.getURI());
//            }
//            logger.debug(bfOntModelInf.getOntClass(
//                    "http://bibframe.org/vocab/Person").getURI());
//        }
    }
    
    public String processAll(List<Action> selectedActions) {
        
        // As we move from one process to another, the output directory becomes
        // the input directory of the next process, and a new output directory
        // for the new process is created.
        String newInputDir = this.mainInputDir;
        String outputDir = null;
        
        logger.trace("STARTING processAll() method");
        logger.trace("mainInputDir = " + mainInputDir);
        logger.trace("newInputDir = " + newInputDir);

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
                logger.trace("newInputDir = " + newInputDir);
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
                logger.trace("STARTING PROCESS " + c.getName());
                logger.trace("newInputDir = " + newInputDir);
                outputDir = processor.process();
                newInputDir = outputDir;
                logger.trace("DONE WITH PROCESS");
                logger.trace("Output dir = " + outputDir);                
            }
        }
        
        // Return path to final results.
        logger.trace("Done!");
        return outputDir;

/* Previous approach where individual processors were called by name. 
        if (selectedActions.contains(Action.CONVERT_BNODES)) {
            
            Processor bnodeConverter = new BNodeToUriConverter(localNamespace,
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
