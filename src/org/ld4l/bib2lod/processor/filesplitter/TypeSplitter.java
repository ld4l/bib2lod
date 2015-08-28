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
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.Ontology;
import org.ld4l.bib2lod.processor.Processor;


public class TypeSplitter extends Processor {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(TypeSplitter.class);
    private static final String outputSubdir = "statementsBySubjectType";
    
    // TODO Figure out if other types should be included here.
    // NB These must be explicitly specified. We don't want to split on all
    // types (e.g., supertypes of these, such as Agent, Authority), and we don't 
    // always want to split on the lowest type in the hierarchy (e.g., we want 
    // to split on Work rather than its subtypes).
    private static final List<String> typesToSplit = Arrays.asList(
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
    
    public TypeSplitter(OntModel bfOntModelInf, String localNamespace, 
            String inputDir, String mainOutputDir) {
        
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);
  
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
            
        String outputDir = createOutputDir(outputSubdir);
        if (outputDir == null) {
            return null;
        }
              
        // After processing all input files, write models to output files
        for (String ontClass: modelsByType.keySet()) {  
            Model model = modelsByType.get(ontClass);
            if (! model.isEmpty()) {
                String outFileName = getFilenameForType(ontClass);                                                                                                                 
                writeModelToFile(outputDir, outFileName, model);
            }
        }      
        
        // Write any remaining triples out to a separate file. 
        // TODO Later these should be analyzed to see if we need to add more 
        // types to split on. For now we just don't want to lose them.
        String remainderFileName = "Other";
                
        writeModelToFile(outputDir, remainderFileName, remainderModel);
        
        return outputDir;
        
    }

    private String getFilenameForType(String uri) {
        return Ontology.BIBFRAME.prefix() + 
                StringUtils.substringAfterLast(uri, "/");
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
            ConstructBuilder cb = new ConstructBuilder() 
                .addConstruct("?s", "?p", "?o")
                .addWhere("?s", "?p", "?o")
                .addWhere("?s", RDF.type, bfOntModelInf.getOntClass(uri));
            Query query = cb.build();
            //logger.debug(query.serialize());
            constructQueriesByType.put(uri, query);
        }
        
        return constructQueriesByType;  
    }
    



}
