package org.ld4l.bib2lod.processor.filesplitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.Namespace;
import org.ld4l.bib2lod.processor.Processor;


public class TypeSplitter extends Processor {

    private static final Logger logger = LogManager.getLogger(TypeSplitter.class);
    private static final String outputSubDir = "statementsBySubjectType";
    
    private final List <String> typesToSplit = Arrays.asList(
            Namespace.BIBFRAME.uri() + "Annotation",
            Namespace.BIBFRAME.uri() + "Family",
            Namespace.BIBFRAME.uri() + "HeldItem",
            Namespace.BIBFRAME.uri() + "Instance",
            Namespace.BIBFRAME.uri() + "Jurisdiction",
            Namespace.BIBFRAME.uri() + "Meeting",
            Namespace.BIBFRAME.uri() + "Organization",
            Namespace.BIBFRAME.uri() + "Person",
            Namespace.BIBFRAME.uri() + "Provider",
            Namespace.BIBFRAME.uri() + "Place",
            Namespace.BIBFRAME.uri() + "Title",
            Namespace.BIBFRAME.uri() + "Topic",   
            Namespace.BIBFRAME.uri() + "Work"           
    );
    private final String remainder = "other";
    
    public TypeSplitter(OntModel bfOntModelInf, String localNamespace, 
            String inputDir, String mainOutputDir) {
        
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);  
    }

    @Override
    public String process() {
        
        String outputDir = createOutputDir(outputSubDir);
        
        // Map each type to a construct query used to populate the model for 
        // that type.
        Map<String, Query> constructQueriesByType = 
                createConstructQueriesByType();
       
        // Map each type to a file for writing output.
        Map<String, File> outputFilesByType = 
                createOutputFilesByType(outputDir);
        
        // For each file in the input directory
        for ( File inputFile : new File(inputDir).listFiles() ) {
            processInputFile(inputFile, constructQueriesByType, 
                    outputFilesByType);           
        }
              
        return outputDir;                                                                                                                               
    }
    
    private Map<String, Query> createConstructQueriesByType() {

        Map<String, Query> constructQueriesByType = 
                new HashMap<String, Query>();

        for (String uri : typesToSplit) {
            // logger.debug("Class URI: " + uri);
            ConstructBuilder cb = new ConstructBuilder() 
                .addConstruct("?s", "?p", "?o")
                .addWhere("?s", "?p", "?o")
                .addWhere("?s", RDF.type, bfOntModelInf.getOntClass(uri));
            Query query = cb.build();
            // logger.debug(query.serialize());
            constructQueriesByType.put(uri, query);
        }
        
        return constructQueriesByType;
    }
    
    private Map<String, File> createOutputFilesByType(String outputDir) {
        
        Map<String, File> outputFilesByType = new HashMap<String, File>();
        
        for (String type : typesToSplit) {
            outputFilesByType.put(type, 
                    createOutputFileForType(outputDir, type));
        }
        
        // Add a file for any remaining triples - i.e., where the subject 
        // doesn't belong to one of the types in typesToSplit.
        outputFilesByType.put(remainder, new File(outputDir, 
                getOutputFilename(remainder)));
        
        return outputFilesByType;
    }

    private File createOutputFileForType(String outputDir, String ontClassUri) {
        
        // Name the file using the namespace prefix plus class localname.
        String localname = StringUtils.substringAfterLast(ontClassUri, "/");
        String namespace = StringUtils.substringBeforeLast(ontClassUri, "/") + "/";
        String prefix = Namespace.getNsPrefix(namespace);
        String basename = prefix + localname;
        return new File(outputDir, getOutputFilename(basename));
    }
    

    private void processInputFile(File inputFile, 
            Map<String, Query> constructQueriesByType,
            Map<String, File> outputFilesByType) {

        // Read RDF from file into a model
        Model inputModel = readModelFromFile(inputFile);
        
        // Map each Bibframe type to a model
        Map<String, Model> modelsByType = createModelsByType();
   
        // For each Bibframe type to split on
        for (String ontClassUri : typesToSplit) {       
            // Get the query and model for this type
            Query query = constructQueriesByType.get(ontClassUri);
            Model model = modelsByType.get(ontClassUri);
            splitByType(query, model, inputModel);            
        }
        
        // Add to the remainder model the statements from this file that 
        // didn't get siphoned off to the type files.
        modelsByType.get(remainder).add(inputModel);
        
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

    private void splitByType(Query query, Model model,
            Model inputModel) {
 
        // Run the query against the input model
        QueryExecution qexec = QueryExecutionFactory.create(
                query, inputModel);
        Model constructModel = qexec.execConstruct();
        qexec.close();

        // Add resulting graph to the model for this type
        model.add(constructModel);
        
        // Remove the resulting graph from the input model, so we
        // don't have to query against those statements on the next 
        // iteration of the loop on this file.
        inputModel.remove(constructModel);        
    }

    private Map<String, Model> createModelsByType() {
        
        Map<String, Model> modelsByType = 
                new HashMap<String, Model>();

        for (String uri: typesToSplit) {
            modelsByType.put(uri, 
                    ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM));     
        }
        modelsByType.put(remainder, 
                ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM));
        
        return modelsByType;
    }


}
