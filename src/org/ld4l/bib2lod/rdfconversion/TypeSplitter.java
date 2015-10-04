package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TypeSplitter extends RdfProcessor {

    private static final Logger LOGGER = 
            LogManager.getLogger(TypeSplitter.class);
    // private static final RdfFormat RDF_OUTPUT_FORMAT = RdfFormat.NTRIPLES;    
    private static final List<OntType> TYPES_TO_SPLIT = 
            UriDeduper.getTypesToDedupe();
    private static final String REMAINDER = UriDeduper.getRemainder();
    
    public TypeSplitter(String localNamespace, String inputDir, 
            String mainOutputDir) {   
        
        super(localNamespace, inputDir, mainOutputDir);  
    }

    @Override
    public String process() {
        
        LOGGER.trace("Start process");
        String outputDir = getOutputDir();

        ParameterizedSparqlString pss = getParameterizedSparqlString();
       
        // Map each type to a file for writing output.
        Map<String, File> outputFilesByType = 
                createOutputFilesByType(outputDir);
        
        // For each file in the input directory
        for ( File inputFile : new File(inputDir).listFiles() ) {
            String filename = inputFile.getName();
            LOGGER.trace("Start processing file " + filename);
            processInputFile(inputFile, pss, outputFilesByType);
            LOGGER.trace("Done processing file " + filename);
        }
           
        LOGGER.trace("End process");
        return outputDir;                                                                                                                               
    }
    
    private ParameterizedSparqlString getParameterizedSparqlString() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        pss.setNsPrefix("bf", OntNamespace.BIBFRAME.uri());
        pss.setNsPrefix("madsrdf", OntNamespace.MADSRDF.uri());
        pss.setCommandText("CONSTRUCT { ?s1 ?p1 ?o1 . "
                + "?o1 ?p2 ?o2 . } "
                + "WHERE {  { " 
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . " 
                + "?o1 a " + OntType.MADSRDF_AUTHORITY.sparqlUri() 
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_IDENTIFIER.sparqlUri()
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + OntType.BF_TITLE.sparqlUri()
                + "} }"                
        );
        return pss;
    }
    
    private Map<String, File> createOutputFilesByType(String outputDir) {
        
        Map<String, File> outputFilesByType = new HashMap<String, File>();
        
        for (OntType type : TYPES_TO_SPLIT) {
            outputFilesByType.put(type.uri(), 
                    createOutputFileForType(outputDir, type));
        }
        
        // Add a file for any remaining triples - i.e., where the subject 
        // doesn't belong to one of the types in typesToSplit.
        // NB This is why the map keys are strings rather than OntologyTypes.
        outputFilesByType.put(REMAINDER, new File(outputDir, 
                getOutputFilename(REMAINDER)));
        
        return outputFilesByType;
    }

    private File createOutputFileForType(String outputDir, OntType type) {
        
        String basename = type.filename();
        return new File(outputDir, getOutputFilename(basename));
    }
    

    private void processInputFile(File inputFile, ParameterizedSparqlString pss,            
            Map<String, File> outputFilesByType) {

        // Read RDF from file into a model
        Model inputModel = readModelFromFile(inputFile);
        
        // Map each Bibframe type to a model
        Map<String, Model> modelsByType = createModelsByType();
   
        // For each Bibframe type to split on
        for (OntType type : TYPES_TO_SPLIT) {       
            String uri = type.uri();
            // Make the type substitution into the parameterized SPARQL string
            pss.setIri("type", uri);
            LOGGER.debug(pss.toString());
            Model modelForType = modelsByType.get(uri);
            splitByType(pss, modelForType, inputModel);            
        }
        
        // Add to the remainder model the statements from this file that 
        // didn't get siphoned off to the type files.
        modelsByType.get(REMAINDER).add(inputModel);
        
        // Append each model to the appropriate file
        writeModelsToFiles(modelsByType, outputFilesByType);
    }
    
    private void writeModelsToFiles(Map<String, Model> modelsByType,             
            Map<String, File> outputFilesByType) {
                  
        for (String ontClassUri: outputFilesByType.keySet()) { 
            File outFile = outputFilesByType.get(ontClassUri);
            Model model = modelsByType.get(ontClassUri);
            if (! model.isEmpty()) {                                                                                                               
                appendModelToFile(model, outFile);
            }
        }          
    }
    
//    @Override
//    protected RdfFormat getRdfOutputFormat() {
//        return RDF_OUTPUT_FORMAT;
//    }

    private void splitByType(ParameterizedSparqlString pss, Model modelForType,
            Model inputModel) {
 
        // Run the query against the input model
        Query query = pss.asQuery();
        LOGGER.debug("Query: " + query.toString());
        QueryExecution qexec = QueryExecutionFactory.create(
                query, inputModel);
        Model constructModel = qexec.execConstruct();
        LOGGER.debug("Model size: " + constructModel.size());
        qexec.close();

        // Add resulting graph to the model for this type
        modelForType.add(constructModel);
        
        // Remove the resulting graph from the input model, so we
        // don't have to query against those statements on the next 
        // iteration of the loop on this file.
        inputModel.remove(constructModel);        
    }

    private Map<String, Model> createModelsByType() {
        
        Map<String, Model> modelsByType = 
                new HashMap<String, Model>();

        for (OntType type: TYPES_TO_SPLIT) {
            modelsByType.put(type.uri(), 
                    ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM));     
        }
        modelsByType.put(REMAINDER, 
                ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM));
        
        return modelsByType;
    }

}
