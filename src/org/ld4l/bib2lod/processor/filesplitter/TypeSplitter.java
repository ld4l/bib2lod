package org.ld4l.bib2lod.processor.filesplitter;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.arq.querybuilder.ConstructBuilder;
import org.apache.jena.ontology.OntModelSpec;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.BibframeType;
import org.ld4l.bib2lod.RdfFormat;
import org.ld4l.bib2lod.processor.Processor;


public class TypeSplitter extends Processor {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(TypeSplitter.class);
    private static final String outputSubdir = "statementsBySubjectType";
    
    // An array-backed list should be fine here; we don't need to modify it. We
    // need to exclude supertypes of the types we want to collect, such as 
    // bf:Authority.
    // TODO Figure out if other types should be included here.
    private static List<BibframeType> typesToSplit = Arrays.asList(
      BibframeType.ANNOTATION,  
      BibframeType.HELD_ITEM,
      BibframeType.INSTANCE,
      BibframeType.ORGANIZATION,
      BibframeType.PERSON,
      BibframeType.PLACE,
      BibframeType.TITLE,
      BibframeType.TOPIC,
      BibframeType.WORK        
    );
    
    public TypeSplitter(String localNamespace, RdfFormat rdfFormat, String inputDir,
            String mainOutputDir) {
        
        super(localNamespace, rdfFormat, inputDir, mainOutputDir);
   
    }

    @Override
    public String process() {

        String outputDir = createOutputDir(outputSubdir);
        if (outputDir == null) {
            return null;
        }
        
        // Map each Bibframe type to an (empty for now) model
        Map<BibframeType, Model> modelsByType = getModelsByType();
        
        // Create another model to accumulate any leftover triples that didn't
        // get put into a type model. 
        // TODO These should be examined later to see if a new type model should
        // be created.
        Model remainderModel = ModelFactory.createDefaultModel();
        
        // For each file in the input directory
        for ( File file : new File(inputDir).listFiles() ) {
            processInputFile(file, modelsByType, remainderModel);
        }
        
        writeModelsToFiles(outputDir, modelsByType, remainderModel);
        
        return outputDir;
                                                                                                                               
    }
    
    private void writeModelsToFiles(String outputDir, Map<BibframeType, 
            Model> modelsByType, Model remainderModel) {
                   
        // After processing all input files, write models to output files
        for (BibframeType type: typesToSplit) {   
            String outFileName = type.localname() + 
                    this.rdfFormat.fullExtension();
            Model modelForType = modelsByType.get(type);                                                                                                                    
            writeModelToFile(outputDir, outFileName, modelForType);

        }      
        
        // Write any remaining triples out to a separate file. 
        // TODO Later these should be analyzed to see if we need to add more 
        // types to split on. For now we just don't want to lose them.
        String remainderFileName = 
                "Other" + this.rdfFormat.fullExtension();
        writeModelToFile(outputDir, remainderFileName, remainderModel);
        
    }

    private void processInputFile(File inputFile, Map<BibframeType, 
            Model> modelsByType, Model remainderModel) {
            
        // Read RDF from file into a model
        Model inputModel = getModelFromFile(inputFile);
        
        // Map each Bibframe type to a construct query used to populate the 
        // model. NB We need the model to create the type resource in the
        // query.
        Map<BibframeType, Query> constructQueriesByType = 
                getConstructQueriesByType(inputModel);
        
        // For each Bibframe type
        for (BibframeType type: typesToSplit) {
            
            // Get the query and model for this type
            Query queryForType = constructQueriesByType.get(type);
            Model modelForType = modelsByType.get(type);
            splitByType(type, queryForType, modelForType, inputModel);
            
        }
        
        // Add to the remainder model the statements from this file that 
        // didn't get siphoned off to the type files.
        remainderModel.add(inputModel);
    }


    private void splitByType(BibframeType type, Query query, Model modelForType,
            Model inputModel) {
 
        // Run the query against the input model
        QueryExecution qexec = QueryExecutionFactory.create(
                query, inputModel);
        Model constructModel = qexec.execConstruct();
        qexec.close();

        // Add resulting graph to the model for this type
        // *** TODO Make sure this modifies the model in the map -
        // modelForType should be a reference
        modelForType.add(constructModel);
        
        // Remove the resulting graph from the input model, so we
        // don't have to query against those statements on the next 
        // iteration of the loop on this file.
        inputModel.remove(constructModel);        
    }

    private Map<BibframeType, Model> getModelsByType() {
        
        Map<BibframeType, Model> modelsByType = 
                new HashMap<BibframeType, Model>();

        for (BibframeType type: typesToSplit) {
            Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            modelsByType.put(type, model);  
        }
        
        return modelsByType;
    }
    
    private Map<BibframeType, Query> getConstructQueriesByType(Model model) {

        Map<BibframeType, Query> constructQueriesByType = 
                new HashMap<BibframeType, Query>();
        
        for (BibframeType type : typesToSplit) {
            Resource typeResource = model.createResource(type.uri());
            ConstructBuilder cb = new ConstructBuilder() 
                .addConstruct("?s", "?p", "?o")
                .addWhere("?s", "?p", "?o")
                .addWhere("?s", RDF.type, typeResource);
            Query typeQuery = cb.build();
            // logger.debug(typeQuery.serialize());
            constructQueriesByType.put(type, typeQuery);
        }
        
        return constructQueriesByType;  
    }


}
