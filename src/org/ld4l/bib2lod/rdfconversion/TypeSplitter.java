package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;

public class TypeSplitter extends RdfProcessor {

    private static final Logger LOGGER = 
            LogManager.getLogger(TypeSplitter.class);
    
    // private static final Format RDF_OUTPUT_FORMAT = Format.NTRIPLES; 
    
    private static final List<BfType> TYPES_TO_SPLIT = Arrays.asList(
            // NB bf:Language must precede bf:Work, since some statements with
            // Work subjects are put in the bf:Language file instead.
            BfType.BF_LANGUAGE,
            BfType.BF_EVENT,
            BfType.BF_FAMILY,
            BfType.BF_HELD_ITEM,
            BfType.BF_INSTANCE,
            BfType.BF_JURISDICTION,
            BfType.BF_MEETING,
            BfType.BF_ORGANIZATION,
            BfType.BF_PERSON,
            BfType.BF_PLACE,
            BfType.BF_TEMPORAL,
            BfType.BF_TOPIC,
            BfType.BF_WORK,
            BfType.BF_AGENT
    );
    
    private static final String REMAINDER_FILENAME = 
            ResourceDeduper.getRemainderFilename();

    
    public TypeSplitter(String inputDir, String mainOutputDir) {   
        super(inputDir, mainOutputDir);  
    }

    @Override
    public String process() {
        
        Instant processStart = Instant.now();
        
        LOGGER.info("Start splitting files by resource type.");
        
        String outputDir = getOutputDir();

        // ParameterizedSparqlString pss = getParameterizedSparqlString();
       
        // Map each type to a file for writing output. The files are constant
        // across input files, and output from successive input files is 
        // appended to each file.
        Map<String, File> outputFilesByType = 
                createOutputFilesByType(outputDir);

        // For logging
        int fileCount = 0;
        Instant fileStart = Instant.now();
        
        // For each file in the input directory
        for ( File inputFile : new File(inputDir).listFiles() ) {
            fileCount++;
            String filename = inputFile.getName();
            LOGGER.trace("Start processing file " + filename);
            processInputFile(inputFile, outputFilesByType);
            LOGGER.trace("Done processing file " + filename);
            
            if (fileCount == TimerUtils.NUM_FILES_TO_TIME) {
                LOGGER.info("Split " + fileCount + " "
                        + Bib2LodStringUtils.simplePlural("file", fileCount)
                        + " by resource type. " 
                        + TimerUtils.getDuration(fileStart));
                fileCount = 0;
                fileStart = Instant.now();   
            }
        }

        if (fileCount > 0) {
            LOGGER.info("Split " + fileCount + " "
                    + Bib2LodStringUtils.simplePlural("file", fileCount)
                    + " by resource type. " 
                    + TimerUtils.getDuration(fileStart)); 
        }   
        
        LOGGER.info("End splitting all files by resource type. " 
                + TimerUtils.getDuration(processStart));
        
        return outputDir;                                                                                                                               
    }

    private Map<String, File> createOutputFilesByType(String outputDir) {
        
        Map<String, File> outputFilesByType = new HashMap<String, File>();
        
        for (BfType type : TYPES_TO_SPLIT) {
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

    private File createOutputFileForType(String outputDir, BfType type) {
        
        String basename = type.filename();
        return new File(outputDir, getOutputFilename(basename));
    }

    private void processInputFile(File inputFile, 
            Map<String, File> outputFilesByType) {          
            
        // Read RDF from file into a model
        Model inputModel = readModelFromFile(inputFile);
        
        // Map each Bibframe type to a model
        Map<String, Model> modelsByType = createModelsByType();
   
        // For each Bibframe type to split on, retrieve from the input model the
        // statements to add to the output file for that type.
        for (BfType type : TYPES_TO_SPLIT) {       
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

        for (BfType type: TYPES_TO_SPLIT) {
            modelsByType.put(type.uri(), 
                    ModelFactory.createDefaultModel());
                    //ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM));     
        }
        modelsByType.put(REMAINDER_FILENAME, 
                ModelFactory.createDefaultModel());
                //ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM));
        
        return modelsByType;
    }

    private void addStatementsForType(BfType type, Model modelForType,
            Model inputModel) {

        Query query = getQueryForType(type);
        
        // Run the query against the input model
        QueryExecution qexec = QueryExecutionFactory.create(
                query, inputModel);
        Model constructModel = qexec.execConstruct();
        qexec.close();
 
//        if (LOGGER.isDebugEnabled()) {
//            LOGGER.debug("Type: " + type.name());
//            LOGGER.debug("Query: " + query.toString());   
//            LOGGER.debug("Model size: " + constructModel.size());
//            RdfProcessor.printModel(constructModel, Level.DEBUG);
//        }

        // Add resulting graph to the model for this type
        modelForType.add(constructModel);
        
        // Remove the resulting graph from the input model, so we
        // don't have to query against those statements on the next 
        // iteration of the loop on this file.
        inputModel.remove(constructModel);        
    }
    
    private Query getQueryForType(BfType type) {
        
        // Type-specific queries
        // Could create separate subclasses, as for dedupers and converters,
        // but it's just a separate query so may not be worth it.
        
        ParameterizedSparqlString pss = null;
        
        if (type.isAuthority()) {
            pss = getBfAuthoritySparql();
        } else if (type == BfType.BF_INSTANCE) {       
            pss = getBfInstanceSparql();
        } else if (type == BfType.BF_WORK) {
            pss = getBfWorkSparql();
        } else if (type == BfType.BF_LANGUAGE) {
            pss = getBfLanguageSparql();
        } else {
            pss = getDefaultSparql();
        }
        
        pss.setNsPrefix("bf", OntNamespace.BIBFRAME.uri());
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        LOGGER.debug(pss.toString());
        return pss.asQuery();
        
    }

    private ParameterizedSparqlString getBfLanguageSparql() {
        
        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("madsrdf", OntNamespace.MADSRDF.uri());

        pss.setCommandText(
                "CONSTRUCT { ?s1 ?p1 ?o1 . "
                + "?work ?p2 ?s1 . "
                + "?work ?p2 ?s2 . } "
                + "WHERE {  { " 
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?work ?p2 ?s1 . " 
                + "?work a " + BfType.BF_WORK.prefixed() + " . "
                + "} UNION { "
                // This returns :work bf:language <external-lang-resource>
                // E.g., :work bf:language 
                // <http://id.loc.gov/vocabulary/languages/eng> .
                // May not be needed now, since we don't do anything with those
                // statements. However, if/when we do lookups against a
                // different external vocabulary - ld4l recommends lingvo - 
                // it may be useful to have them all together in a single file.
//                + "?work ?p2 ?s2 . "
//                + "?work a " + BfType.BF_WORK.prefixed() + " . "
//                + "?work " + BfProperty.BF_LANGUAGE.prefixed() + " ?s2 . " 
                + "} }"                
        );
        
        return pss;
    }

    
    private ParameterizedSparqlString getBfAuthoritySparql() {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        pss.setNsPrefix("madsrdf", OntNamespace.MADSRDF.uri());

        pss.setCommandText(
                "CONSTRUCT { ?s1 ?p1 ?o1 . "
                + "?o1 ?p2 ?o2 . " 
                + "?work ?p3 ?s1 } "
                + "WHERE {  { " 
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "} UNION { "
                + "?s1 " + BfProperty.BF_HAS_AUTHORITY.prefixed() + " ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . " 
                + "?o1 a " + BfType.MADSRDF_AUTHORITY.prefixed()  
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_IDENTIFIER.prefixed()  
                + "} UNION { "
                // Include the :work bf:subject :authority statement here rather
                // than with the work, since it affects the conversion of the
                // authority.
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?work ?p3 ?s1 . "
                + "FILTER (str(?p3) = \"" + BfProperty.BF_SUBJECT.uri() 
                + "\") "
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
                + "?o1 a " + BfType.BF_IDENTIFIER.prefixed() 
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_TITLE.prefixed()
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_PROVIDER.prefixed() 
                + "} UNION { "
                + "?s1 " + BfProperty.BF_HAS_ANNOTATION.prefixed() + " ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_ANNOTATION.prefixed() 
                + "} UNION { "
                // LC converter typically generates 
                // :anno bf:annotates :resource rather than
                // :resource bf:hasAnnotation :anno
                + "?o1 " + BfProperty.BF_ANNOTATES.prefixed() +  " ?s1 . "
                + "?s1 a ?type . " 
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_ANNOTATION.prefixed()
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_CLASSIFICATION.prefixed()  
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_CATEGORY.prefixed() 
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
                // These statements need to be included in the file for the
                // object rather than the Work subject, UNLESS the object is 
                // itself a Work, in which case it should be added to the 
                // Work file.
                + "FILTER ( (str(?p1) != \"" + BfProperty.BF_SUBJECT.uri() 
                + "\" ) || EXISTS { ?o1 a ?type } ) "
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_IDENTIFIER.prefixed() 
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_TITLE.prefixed()              
                + "} UNION { "
                + "?s1 " + BfProperty.BF_HAS_ANNOTATION.prefixed() + " ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_ANNOTATION.prefixed() 
                + "} UNION { "
                // LC converter typically generates 
                // :anno bf:annotates :resource rather than
                // :resource bf:hasAnnotation :anno
                + "?o1 " + BfProperty.BF_ANNOTATES.prefixed() +  " ?s1 . "
                + "?s1 a ?type . " 
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_ANNOTATION.prefixed()
                + "} UNION {"
                + "?o1 " + BfProperty.BF_REVIEW_OF.prefixed() +  " ?s1 . "
                + "?s1 a ?type . " 
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_ANNOTATION.prefixed()
                + "} UNION {"
                + "?o1 " + BfProperty.BF_SUMMARY_OF.prefixed() +  " ?s1 . "
                + "?s1 a ?type . " 
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_ANNOTATION.prefixed()
                + "} UNION { "
                + "?s1 ?p1 ?o1 . "
                + "?s1 a ?type . "
                + "?o1 ?p2 ?o2 . "
                + "?o1 a " + BfType.BF_CLASSIFICATION.prefixed()                 
                + "} }"                
        );
        LOGGER.debug(pss.toString());
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
