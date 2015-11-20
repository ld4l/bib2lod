package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BnodeConverter extends RdfProcessor {

    private static final Logger LOGGER = 
            LogManager.getLogger(BnodeConverter.class);
   
    
    public BnodeConverter(String localNamespace, 
            String inputDir, String mainOutputDir) {
                        
        super(localNamespace, inputDir, mainOutputDir);
    }

    @Override
    public String process() {        
        
        LOGGER.info("Start converting blank nodes to URIs.");
        String outputDir = getOutputDir();

        int fileCount = 0;
        for ( File file : new File(inputDir).listFiles() ) {
            String filename = file.getName();
            LOGGER.trace("Start processing file " + filename);
            fileCount++;
            Model outputModel = processInputFile(file, fileCount);
            // Write out to same filename as input file
            String basename = FilenameUtils.getBaseName(file.toString());
            writeModelToFile(outputModel, basename);
            LOGGER.trace("Done processing file " + filename);
        }   
        
        LOGGER.info("End converting blank nodes to URIs.");
        return outputDir;
    }
    
    private Model processInputFile(File inputFile, int fileCount) {
        
        Model inputModel = readModelFromFile(inputFile);
        Model assertions = ModelFactory.createDefaultModel();
        Model retractions = ModelFactory.createDefaultModel();
        Map<String, Resource> bnodeIdToUriResource = 
                new HashMap<String, Resource>();
        StmtIterator statements = inputModel.listStatements();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            convertBnodesToUris(statement, assertions, retractions, 
                    bnodeIdToUriResource, fileCount);
        }
        inputModel.remove(retractions);
        inputModel.add(assertions);   
        if (LOGGER.isDebugEnabled()) {
            for (Map.Entry<String, Resource> entry : 
                    bnodeIdToUriResource.entrySet()) {
                LOGGER.debug(entry.getKey() + ": " 
                        + entry.getValue().toString());
            }
        }
        return inputModel;
    }

    private void convertBnodesToUris(Statement statement, Model assertions,
            Model retractions, Map<String, Resource> bnodeIdToUriResource,
            int fileCount) {
        
        Resource subject = statement.getSubject();
        Property property = statement.getPredicate();
        RDFNode object = statement.getObject();
        if (! (subject.isAnon() || object.isAnon()) ) {
            return;
        }
        Resource newSubject = subject;
        RDFNode newObject = object;
        if (subject.isAnon()) {
            newSubject = createUriResourceForAnonNode(subject, 
                    bnodeIdToUriResource, assertions, fileCount);
        }
        if (object.isAnon()) {
            newObject = createUriResourceForAnonNode(object, 
                    bnodeIdToUriResource, assertions, fileCount);               
        }
        retractions.add(subject, property, object);
        // This handles cases where both subject and object are blank nodes.
        assertions.add(newSubject, property, newObject);
    }        
    
    
    private Resource createUriResourceForAnonNode(RDFNode rdfNode, 
            Map<String, Resource> idToUriResource, Model assertions, 
            int fileCount) {
        Node node = rdfNode.asNode();
        /*
         * Prepend fileCount to blank node label to ensure that URIs are not
         * duplicated across files. Blank node ids are locally scoped to a file
         * and thus may be duplicated across files, but we do not want 
         * convergence of the same blank node id across files. Note: The LC 
         * converter actually creates unique bnode ids by appending the bib id, 
         * much in the way that unique URIs are created, but when Jena reads the 
         * RDF file into a model, it generates its own bnode ids that are 
         * locally scoped to the file.
         */
        String id = fileCount + "_" + node.getBlankNodeId().toString();
        Resource uriResource;
        if (idToUriResource.containsKey(id)) {
            uriResource = idToUriResource.get(id);  
            LOGGER.debug("Found hash key " + id);
        } else {
            // Use Java UUID to mint new URIs
            // uriResource = assertions.createResource(convertLabelToUri(id));
            uriResource = assertions.createResource(mintUri(localNamespace));
            idToUriResource.put(id, uriResource);
            LOGGER.debug("Creating new hash entry for id " + id);
        }
        return uriResource;
    }
    
//    private String convertLabelToUri(String id) {
//        String localName = id.replaceAll("\\W", "");
//        return localNamespace + localName;
//    }


}
