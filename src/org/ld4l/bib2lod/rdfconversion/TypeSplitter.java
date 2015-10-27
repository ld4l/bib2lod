package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TypeSplitter extends RdfProcessor {

    private static final Logger LOGGER = 
            LogManager.getLogger(TypeSplitter.class);
    
    // private static final Format RDF_OUTPUT_FORMAT = Format.NTRIPLES; 
    
    private static final List<OntType> TYPES_TO_SPLIT = 
            ResourceDeduper.getTypesToDedupe();
    
    private static final String REMAINDER_FILENAME = 
            ResourceDeduper.getRemainderFilename();

    
    public TypeSplitter(String inputDir, String mainOutputDir) {   
        super(inputDir, mainOutputDir);  
    }

    @Override
    public String process() {
        
        LOGGER.info("Start process");
        String outputDir = getOutputDir();

        //ParameterizedSparqlString pss = getParameterizedSparqlString();
       
        // Map each type to a file for writing output. The files are constant
        // across input files, and output from successive input files is 
        // appended to each file.
        Map<String, File> outputFilesByType = 
                createOutputFilesByType(outputDir);
        
        // For each file in the input directory
        for ( File inputFile : new File(inputDir).listFiles() ) {
            String filename = inputFile.getName();
            LOGGER.info("Start processing file " + filename);
            processInputFile(inputFile, outputFilesByType);
            LOGGER.info("Done processing file " + filename);
        }
           
        LOGGER.info("End process");
        return outputDir;                                                                                                                               
    }

    private Map<String, File> createOutputFilesByType(String outputDir) {
        
        Map<String, File> outputFilesByType = new HashMap<String, File>();
        
        for (OntType type : TYPES_TO_SPLIT) {
            outputFilesByType.put(type.uri(), 
                    createOutputFileForType(outputDir, type));
        }
        
        // Add a file for any remaining triples.
        // NB The goal is to eliminate this file; all triples should end up in
        // one of the other files. 
        outputFilesByType.put(REMAINDER_FILENAME, new File(outputDir, 
                getOutputFilename(REMAINDER_FILENAME)));
        
        return outputFilesByType;
    }

    private File createOutputFileForType(String outputDir, OntType type) {
        
        String basename = type.filename();
        return new File(outputDir, getOutputFilename(basename));
    }

    private void processInputFile(File inputFile, 
            Map<String, File> outputFilesByType) {          
            
        // Read RDF from file into a model
        Model inputModel = readModelFromFile(inputFile);
        
        // Map each Bibframe type to a model
        Map<String, Model> modelsByType = createModelsByType();
   
        // For each Bibframe type to split on, retrive from the input model the
        // statements to add to the output file for that type.
        for (OntType type : TYPES_TO_SPLIT) {       
            Model modelForType = modelsByType.get(type.uri());
            addStatementsForType(type, modelForType, inputModel);
        }
        
        // Add to the remainder model the statements from this file that 
        // didn't get siphoned off to the type files.
        // NB The goal is to eliminate these by refining the queries to include
        // all statements in the models for types.
        modelsByType.get(REMAINDER_FILENAME).add(inputModel);
        
        // Append each model to the appropriate file
        writeModelsToFiles(modelsByType, outputFilesByType);
    }

    private Map<String, Model> createModelsByType() {
        
        Map<String, Model> modelsByType = 
                new HashMap<String, Model>();

        for (OntType type: TYPES_TO_SPLIT) {
            modelsByType.put(type.uri(), 
                    ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM));     
        }
        modelsByType.put(REMAINDER_FILENAME, 
                ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM));
        
        return modelsByType;
    }

    private void addStatementsForType(OntType type, Model modelForType,
            Model inputModel) {

        Query query = getQueryForType(type);
        
        // Run the query against the input model
        QueryExecution qexec = QueryExecutionFactory.create(
                query, inputModel);
        Model constructModel = qexec.execConstruct();
        qexec.close();
 
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Type: " + type.name());
            LOGGER.debug("Query: " + query.toString());   
            LOGGER.debug("Model size: " + constructModel.size());
            RdfProcessor.printModel(constructModel, Level.DEBUG);
        }

        // Add resulting graph to the model for this type
        modelForType.add(constructModel);
        
        // Remove the resulting graph from the input model, so we
        // don't have to query against those statements on the next 
        // iteration of the loop on this file.
        inputModel.remove(constructModel);        
    }
    
    private Query getQueryForType(OntType type) {
        
        // Type-specific queries
        // Could create separate subclasses, as for dedupers and converters,
        // but it's just a separate query so may not be worth it.
        
        ParameterizedSparqlString pss = null;
        
        if (type == OntType.BF_TOPIC) {          
            pss = getBfTopicSparql();
        } else if (OntType.authorities().contains(type)) {
            pss = getBfAuthoritySparql();
        } else if (type == OntType.BF_INSTANCE) {       
            pss = getBfInstanceSparql();
        } else if (type == OntType.BF_WORK) {
            pss = getBfWorkSparql();
        } else {
            pss = getDefaultSparql();
        }
        
        pss.setNsPrefix("bf", OntNamespace.BIBFRAME.uri());
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        return pss.asQuery();
        
    }

    private ParameterizedSparqlString getBfTopicSparql() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("madsrdf", OntNamespace.MADSRDF.uri());

        pss.setCommandText(
                "CONSTRUCT { ?s1 ?p1 ?o1 . "
                + "?o1 ?p2 ?o2 . } "
                + "WHERE {  { " 
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "} UNION { "
                + "?s1 " + OntProperty.BF_HAS_AUTHORITY.prefixed() + " ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . " 
                + "?o1 a " + OntType.MADSRDF_AUTHORITY.prefixed() 
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_IDENTIFIER.prefixed()                 
                + "} }"                
        );
        
        return pss;
    }
    
    private ParameterizedSparqlString getBfAuthoritySparql() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("madsrdf", OntNamespace.MADSRDF.uri());

        pss.setCommandText(
                "CONSTRUCT { ?s1 ?p1 ?o1 . "
                + "?o1 ?p2 ?o2 . } "
                + "WHERE {  { " 
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "} UNION { "
                + "?s1 " + OntProperty.BF_HAS_AUTHORITY.prefixed() + " ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . " 
                + "?o1 a " + OntType.MADSRDF_AUTHORITY.prefixed()               
                + "} }"                
        );
        
        return pss;
    }
    
    private ParameterizedSparqlString getBfInstanceSparql() {
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        pss.setCommandText(
                "CONSTRUCT { ?s1 ?p1 ?o1 . "
                + "?o1 ?p2 ?o2 . } "
                + "WHERE {  { " 
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_IDENTIFIER.prefixed() 
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_TITLE.prefixed()
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_PROVIDER.prefixed() 
                + "} UNION { "
                + "?s1 " + OntProperty.BF_HAS_ANNOTATION.prefixed() + " ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_ANNOTATION.prefixed() 
                + "} UNION { "
                // LC converter typically generates 
                // :anno bf:annotates :resource rather than
                // :resource bf:hasAnnotation :anno
                + "?o1 " + OntProperty.BF_ANNOTATES.prefixed() +  " ?s1 . "
                + "?s1 a ?type . " 
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_ANNOTATION.prefixed()
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_CLASSIFICATION.prefixed()                 
                + "} }"                
        );
        
        return pss;
    }

    private ParameterizedSparqlString getBfWorkSparql() {
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        pss.setCommandText(
                "CONSTRUCT { ?s1 ?p1 ?o1 . "
                + "?o1 ?p2 ?o2 . } "
                + "WHERE {  { " 
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_IDENTIFIER.prefixed() 
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_TITLE.prefixed()
                + "} UNION { "
                + "?s1 " + OntProperty.BF_HAS_ANNOTATION.prefixed() + " ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_ANNOTATION.prefixed() 
                + "} UNION { "
                // LC converter typically generates 
                // :anno bf:annotates :resource rather than
                // :resource bf:hasAnnotation :anno
                + "?o1 " + OntProperty.BF_ANNOTATES.prefixed() +  " ?s1 . "
                + "?s1 a ?type . " 
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_ANNOTATION.prefixed()
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_CLASSIFICATION.prefixed()                 
                + "} }"                
        );
        
        return pss;
        
    }
    
    private ParameterizedSparqlString getDefaultSparql() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        pss.setCommandText(
                "CONSTRUCT { ?s1 ?p1 ?o1 . "
                + "?o1 ?p2 ?o2 . } "
                + "WHERE { " 
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "}"                
        );
        
        return pss;
    }
 
    private void writeModelsToFiles(Map<String, Model> modelsByType,             
            Map<String, File> outputFilesByType) {
                  
        for (Map.Entry<String, File> entry : outputFilesByType.entrySet()) {
            String ontClassUri = entry.getKey();
            File outFile = entry.getValue();
            Model model = modelsByType.get(ontClassUri);
            if (! model.isEmpty()) {                                                                                                               
                appendModelToFile(model, outFile);
            }
        }          
    }
    
}
