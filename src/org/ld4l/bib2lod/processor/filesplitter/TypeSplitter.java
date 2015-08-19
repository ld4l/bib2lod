package org.ld4l.bib2lod.processor.filesplitter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
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
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
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
            
            // TODO - put loop body into method processRdfFile
            
            // Read RDF from file into a model
            Model inputModel = getModelFromFile(file);
            
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

            }
        }
        
        // TODO Put this in a separate method
        // After processing all files, write models out to files using
        // type.localname() + "." + this.rdfFormat.fullExtension();
        // and a Jena method to write out the model
        for (BibframeType type: typesToSplit) {
            
            Model modelForType = modelsByType.get(type);
            
            String outFileName = type.localname() + this.rdfFormat.fullExtension();
            
            // TODO From here on can be a Processor method
            File outFile = new File(outputDir, outFileName);
            
            try {
                FileOutputStream outStream = new FileOutputStream(outFile);
                RDFDataMgr.write(outStream, modelForType, this.rdfFormat.rdfFormat());
            } catch (FileNotFoundException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

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
    
    private Map<BibframeType, Query> getConstructQueriesByType() {
      
        // Using a base query builder, cloning it, and adding on to it doesn't
        // work, as the base query gets modified with the clone.
//        ConstructBuilder baseCb = new ConstructBuilder()
//            .addConstruct("?s", "?p", "?o")
//            .addWhere("?s", "?p", "?o");

        Map<BibframeType, Query> constructQueriesByType = 
                new HashMap<BibframeType, Query>();
        
        for (BibframeType type : typesToSplit) {
            ConstructBuilder cb = new ConstructBuilder() 
                .addConstruct("?s", "?p", "?o")
                .addWhere("?s", "?p", "?o")
                .addWhere("?s", RDF.type,   type.uri());
            Query typeQuery = cb.build();
            logger.debug(typeQuery.serialize());
            constructQueriesByType.put(type, typeQuery);
        }
        
        return constructQueriesByType;  
    }


}
