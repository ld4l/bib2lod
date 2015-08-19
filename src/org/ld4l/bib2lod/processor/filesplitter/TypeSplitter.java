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

    private static final Logger logger = LogManager.getLogger(TypeSplitter.class);
    private static final String outputSubdir = "statementsBySubjectType";
    
    // An array-backed list should be fine here; we don't need to modify it.
    // TODO Figure out which other types to include here
    private static List<BibframeType> typesToSplit = Arrays.asList(
      BibframeType.ANNOTATION,  
      BibframeType.HELD_ITEM,
      BibframeType.INSTANCE,
      BibframeType.ORGANIZATION,
      BibframeType.PERSON,
      BibframeType.TITLE,
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
            
            // TODO - put loop body into method processRdfFile
            
            // Read RDF from file into a model
            Model inputModel = getModelFromFile(file);
            
            // Map each Bibframe type to a construct query used to populate the 
            // model. NB We need the model to create the type resource in the
            // query.
            Map<BibframeType, Query> constructQueriesByType = 
                    getConstructQueriesByType(inputModel);
            
            // logger.debug(showStatements(inputModel));
            
            // For each Bibframe type
            for (BibframeType type: typesToSplit) {
                
                // TODO Put loop body into a method processType

                // Get the construct query for this type
                Query queryForType = constructQueriesByType.get(type);
                
                // Run the query against the input model
                QueryExecution qexec = QueryExecutionFactory.create(
                        queryForType, inputModel);
                Model resultModel = qexec.execConstruct();
                logger.debug(showStatements(resultModel));
                qexec.close();
                
                // Get the model for this type
                Model modelForType = modelsByType.get(type);
                
                // Add resulting graph to the model for this type
                // *** TODO Make sure this modifies the model in the map -
                // modelForType should be a reference
                modelForType.add(resultModel);
                
                // Remove the resulting graph from the input model, so we
                // don't have to query against those statements again.
                inputModel.remove(resultModel);
                
                // Store the remaining triples so we don't lose them.
                remainderModel.add(inputModel);

            }
        }
        
        // After processing all files, write models out to files using
        // type.localname() + "." + this.rdfFormat.fullExtension();
        // and a Jena method to write out the model
        for (BibframeType type: typesToSplit) {   
            String outFileName = type.localname() + 
                    this.rdfFormat.fullExtension();
            Model modelForType = modelsByType.get(type);                                                                                                                    
            writeModelToFile(outputDir, outFileName, modelForType);

        }      
        
        // Write the remaining triples out to a separate file
        String remainderFileName = 
                "Miscellaneous" + this.rdfFormat.fullExtension();
        writeModelToFile(outputDir, remainderFileName, remainderModel);

        return outputDir;
                                                                                                                               
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
            logger.debug(typeQuery.serialize());
            constructQueriesByType.put(type, typeQuery);
        }
        
        return constructQueriesByType;  
    }


}
