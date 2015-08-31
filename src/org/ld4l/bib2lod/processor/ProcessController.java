package org.ld4l.bib2lod.processor;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.Action;
import org.ld4l.bib2lod.Ontology;
import org.ld4l.bib2lod.processor.filesplitter.TypeSplitter;



public class ProcessController {

    private static final Logger logger = LogManager.getLogger(ProcessController.class);

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

        // TODO Needs a total redo. Ontologies and filenames should not be
        // hard-coded. But if we loop through the rdf directory, we don't get
        // named references to the models.
        // See https://jena.apache.org/documentation/ontology/#creating-ontology-models
        // on managing file references. Use Jena's DocumentManager and
        // FileManager.
        String rdfDir = "rdf";
        
        File bfOntFile = new File(rdfDir, Ontology.BIBFRAME.filename());
        OntModel bfOntModelBase = 
                ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        bfOntModelBase.read(bfOntFile.toString(), "RDF/XML"); 
        this.bfOntModelInf = ModelFactory.createOntologyModel(
                OntModelSpec.OWL_MEM_MICRO_RULE_INF, bfOntModelBase);
        
//        File ld4lOntFile = new File(rdfDir, Ontology.LD4L.filename());
//        OntModel ld4lOntModelBase = 
//                  ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
//        ld4lBase.read(ld4lOntFile.toString(), "RDF/XML"); 
//        this.ld4lOntModelInf = ModelFactory.createOntologyModel(
//                OntModelSpec.OWL_MEM_MICRO_RULE_INF, ld4lOntModelBase);  

//        if (logger.isDebugEnabled()) {
//            String bfNamespace = Ontology.BIBFRAME.namespace();
//            OntClass person = bfBase.getOntClass(bfNamespace  + "Person");
//            Individual p1 = bfBase.createIndividual(bfNamespace + "person1", person);
//            
//            for (Iterator<Resource> i = p1.listRDFTypes(true); i.hasNext();) {
//                logger.debug(p1.getURI() + " is asserted in class " + i.next());
//            }
//            
//            p1 = bfOntModelInf.getIndividual(bfNamespace + "person1");
//            for (Iterator<Resource> i = p1.listRDFTypes(false); i.hasNext();) {
//                // NB This includes the asserted as well as the inferred classes.
//                logger.debug(p1.getURI() + " is inferred to be in class " + i.next());
//            }
//            
//            for (Iterator<Resource> i = p1.listRDFTypes(true); i.hasNext();) {
//                // NB This includes the asserted as well as the inferred classes.
//                logger.debug(p1.getURI() + " is directly inferred to be in class " + i.next());
//            }
//            
//            OntClass personClass = bfOntModelInf.getOntClass(bfNamespace + "Person");
//            for (Iterator<OntClass> i = personClass.listSuperClasses(); i.hasNext();) {
//                logger.debug(personClass.getURI() + " is a subclass of " + i.next());
//            }           
//        }
    }
    
    public String processAll(List<Action> selectedActions) {
        
        // As we move from one process to another, the output directory becomes
        // the input directory of the next process, and a new output directory
        // for the new process is created.
        String outputDir = this.inputDir;
        
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
                try {
                    constructor = c.getConstructor(
                            OntModel.class, String.class, String.class, 
                            String.class);
                    processor = (Processor) constructor.newInstance(
                            bfOntModelInf,localNamespace, outputDir, 
                            mainOutputDir);
                } catch (NoSuchMethodException e) {
                    try {
                        constructor = c.getConstructor(String.class, 
                                String.class, String.class);
                        processor = (Processor) constructor.newInstance(
                                localNamespace, outputDir, mainOutputDir);          
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        return null;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    return null;
                }
                logger.debug("Output dir = " + outputDir);
                outputDir = processor.process();
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
