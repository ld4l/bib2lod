package org.ld4l.bib2lod.processor;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import org.apache.jena.ontology.Individual;
import org.apache.jena.ontology.OntClass;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.Action;
import org.ld4l.bib2lod.Ontology;
import org.ld4l.bib2lod.RdfFormat;
import org.ld4l.bib2lod.processor.filesplitter.TypeSplitter;



public class ProcessController {

    private static final Logger logger = LogManager.getLogger(ProcessController.class);
    
    protected String localNamespace;
    protected RdfFormat rdfFormat;
    
    protected String inputDir;
    protected String mainOutputDir;
    
    protected OntModel bfOntModelInf;
    protected OntModel ld4lInf;
    
    public ProcessController(String localNamespace, RdfFormat rdfFormat, 
            String inputDir, String outputDir) {
        
        this.localNamespace = localNamespace;
        this.rdfFormat = rdfFormat;
        
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
        OntModel bfBase = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        bfBase.read(bfOntFile.toString(), "RDF/XML"); 
        this.bfOntModelInf = ModelFactory.createOntologyModel(
                OntModelSpec.OWL_MEM_MICRO_RULE_INF, bfBase);
        
        File ld4lOntFile = new File(rdfDir, Ontology.LD4L.filename());
        OntModel ld4lBase = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
        ld4lBase.read(ld4lOntFile.toString(), "RDF/XML"); 
        this.ld4lInf = ModelFactory.createOntologyModel(
                OntModelSpec.OWL_MEM_MICRO_RULE_INF, ld4lBase);  

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
    
    public String processAll(List<Action> actions) {
        
        // As we move from one process to another, the output directory becomes
        // the input directory of the next process, and a new output directory
        // for the new process is created.
        String outputDir = null;
        String currentInputDir = this.inputDir;
        
        // TODO Implement earlier actions: marcxml pre-processing, 
        // marcxml2bibframe conversion, etc.
        // Use Action rank to order the actions
        
        // Correct errors in the Bibframe RDF that choke the ingest process.
        // Could do this after deduping, but probably these corrections should 
        // be included in deduped data.

        // NB If there are earlier actions, the TypeSplitter constructor gets
        // passed in the resultsDir of the previous process, not this.inputPath.
        
        if (actions.contains(Action.DEDUPE_BIBFRAME_URIS)) {

            // TODO Maybe: add another process to convert blank nodes to URI 
            // resources. Not sure if needed.
//            BlankNodeToUriConverter bnodeConverter = 
//                    new BlankNodeToUriConverter(bfOntModelInf, localNamespace, 
//                            rdfFormat, currentInputDir, mainOutputDir);
            
            TypeSplitter splitter = new TypeSplitter(bfOntModelInf, localNamespace, rdfFormat, 
                    currentInputDir, mainOutputDir);
            outputDir = splitter.process();
        }
        
        // TODO Insert other processes here
        // currentInputDir = previous outputDir
        // New outputDir defined by processor.
        // Stages:
        // Convert Bibframe RDF to LD4L RDF
        // Entity resolution
        

        // Return path to final results.
        logger.trace("Done!");
        return outputDir;

    }


}
