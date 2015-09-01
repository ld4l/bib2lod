package org.ld4l.bib2lod.processor.filesplitter;

import java.io.File;
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
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.Ontology;
import org.ld4l.bib2lod.processor.Processor;


public class TypeSplitter extends Processor {

    private static final Logger logger = LogManager.getLogger(TypeSplitter.class);
    private static final String outputSubDir = "statementsBySubjectType";
    
    private static List<String> typesToSplit;
    
    public TypeSplitter(OntModel bfOntModelInf, String localNamespace, 
            String inputDir, String mainOutputDir) {
        
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);
        
        typesToSplit = Arrays.asList(
            Ontology.BIBFRAME.namespace() + "Annotation",
            Ontology.BIBFRAME.namespace() + "Family",
            Ontology.BIBFRAME.namespace() + "HeldItem",
            Ontology.BIBFRAME.namespace() + "Instance",
            Ontology.BIBFRAME.namespace() + "Jurisdiction",
            Ontology.BIBFRAME.namespace() + "Meeting",
            Ontology.BIBFRAME.namespace() + "Organization",
            Ontology.BIBFRAME.namespace() + "Person",
            Ontology.BIBFRAME.namespace() + "Place",
            Ontology.BIBFRAME.namespace() + "Title",
            Ontology.BIBFRAME.namespace() + "Topic",   
            Ontology.BIBFRAME.namespace() + "Work" 
        );

    }

    @Override
    public String process() {

        // Map each Bibframe type to an (empty for now) model
        Map<String, Model> modelsByType = getModelsByType();

        // Create another model to accumulate any leftover triples that didn't
        // get put into a type model. 
        // TODO These should be examined later to see if a new type model should
        // be created.
        Model remainderModel = ModelFactory.createDefaultModel();
        
        // Map each Bibframe type to a construct query used to populate the 
        // model. NB We need the model to create the type resource in the
        // query.
        Map<String, Query> constructQueriesByType = getConstructQueriesByType();
                
        // For each file in the input directory
        for ( File file : new File(inputDir).listFiles() ) {
            processInputFile(file, modelsByType, remainderModel, 
                    constructQueriesByType);
        }
        
        String outputDir = writeModelsToFiles(modelsByType, remainderModel);
                      
        return outputDir;
                                                                                                                               
    }
    
    private String writeModelsToFiles(Map<String, Model> modelsByType, 
            Model remainderModel) {
            
        String outputDir = createOutputDir(outputSubDir);
        if (outputDir == null) {
            return null;
        }
              
        // After processing all input files, write models to output files
        for (String ontClassUri: modelsByType.keySet()) {  
            Model model = modelsByType.get(ontClassUri);
            if (! model.isEmpty()) {
                String outFileName = getFilenameForType(ontClassUri, model);                                                                                                                 
                writeModelToFile(model, outputDir, outFileName);
            }
        }      
        
        // Write any remaining triples out to a separate file. 
        // TODO Later these should be analyzed to see if we need to add more 
        // types to split on. For now we just don't want to lose them.
        String remainderFileName = "Other";
                
        writeModelToFile(remainderModel, outputDir, remainderFileName);
        
        return outputDir;
        
    }

    private String getFilenameForType(String ontClassUri, Model model) { 
        // Get uri with prefix rather than full namespace
        String shortUri = model.shortForm(ontClassUri);
        return shortUri.replace(":", "");              
    }

    private void processInputFile(File inputFile, 
            Map<String, Model> modelsByType, Model remainderModel, 
            Map<String, Query> constructQueriesByType) {
            
        // Read RDF from file into a model
        Model inputModel = readModelFromFile(inputFile);
   
        // For each Bibframe type
        // for (String uri: typesToSplit) {       
        for (String ontClass : constructQueriesByType.keySet()) {
            // Get the query and model for this type
            Query query = constructQueriesByType.get(ontClass);
            Model model = modelsByType.get(ontClass);
            splitByType(query, model, inputModel);            
        }
        
        // Add to the remainder model the statements from this file that 
        // didn't get siphoned off to the type files.
        remainderModel.add(inputModel);
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

    private Map<String, Model> getModelsByType() {
        
        Map<String, Model> modelsByType = 
                new HashMap<String, Model>();

        for (String uri: typesToSplit) {
            modelsByType.put(uri, 
                    ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM));     
        }

        return modelsByType;
    }
    
    private Map<String, Query> getConstructQueriesByType() {

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

}
