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
      BibframeType.INSTANCE,
      BibframeType.PERSON,
      BibframeType.TITLE,
      BibframeType.WORK        
    );
    
//    private static String queryTemplate = 
//            "CONSTRUCT ?s ?p ?o "
//            + "WHERE { "
//            + "?s ?p ?o . "
//            + "?s a <TYPE> . "
//            + "}";
    
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
        
        // Map each Bibframe type to a construct query used to populate the 
        // model.
        Map<BibframeType, Query> constructQueriesByType = 
                getConstructQueriesByType();

//        File inputFiles = new File(inputDir);
//        File[] files = inputFiles.listFiles();
        
        // For each file in the input directory
        for ( File file : new File(inputDir).listFiles() ) {
            
            // Read RDF from file into a model
            Model inputModel = getModelFromFile(file);
            
            // TODO - put this loop into another method
            
            // For each Bibframe type
            for (BibframeType type: typesToSplit) {
                // String uri = type.uri();
                
                // Get the model for this type
                Model modelForType = modelsByType.get(type);
                
                // Get the construct query for this type
                Query queryForType = constructQueriesByType.get(type);
                
                // Run the query against the input model
                QueryExecution qexec = QueryExecutionFactory.create(
                        queryForType, inputModel);
                Model resultModel = qexec.execConstruct() ;
                qexec.close() ;

                // Add resulting graph to the model for this type
                modelForType.add(resultModel);
                
                // Remove the resulting graph from the input model, so we
                // don't have to query against those statements again.
                inputModel.remove(resultModel);

            }
            // In another method:
            /* TODO
             * after iterating through all files, write model to file using
             * filename:
             * type.localname() + "." + this.rdfFormat.fullExtension();
             * and a Jena method to write out the model 
             */
        }

        return outputDir;

    }
    
    private 
    
    private Map<BibframeType, Model> getModelsByType() {
        
        Map<BibframeType, Model> modelsByType = 
                new HashMap<BibframeType, Model>();

        for (BibframeType type: typesToSplit) {
            Model model = ModelFactory.createOntologyModel(OntModelSpec.OWL_MEM);
            modelsByType.put(type, model);  
        }
        
        return modelsByType;
    }
    
    private Map<BibframeType, Query> getConstructQueriesByType() {
      
        ConstructBuilder baseCb = new ConstructBuilder()
            .addConstruct("?s", "?p", "?o")
            .addWhere("?s", "?p", "?o");
        
        //Query query = constructBuilder.build();

        Map<BibframeType, Query> constructQueriesByType = 
                new HashMap<BibframeType, Query>();
        
        for (BibframeType type : typesToSplit) {
            ConstructBuilder cb = baseCb.clone();
            cb.addWhere("?s", RDF.type, type);
            Query typeQuery = cb.build();
            constructQueriesByType.put(type, typeQuery);
        }
        
        return constructQueriesByType;  
    }


}
