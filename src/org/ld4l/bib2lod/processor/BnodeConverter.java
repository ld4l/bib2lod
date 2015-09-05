package org.ld4l.bib2lod.processor;

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

public class BnodeConverter extends Processor {

    private static final Logger LOGGER = 
            LogManager.getLogger(BnodeConverter.class);
   
    
    public BnodeConverter(String localNamespace, 
            String inputDir, String mainOutputDir) {
                        
        super(localNamespace, inputDir, mainOutputDir);

    }

    @Override
    public String process() {        
        
        String outputDir = createOutputDir(outputSubdir);
        
        for ( File file : new File(inputDir).listFiles() ) {
            Model outputModel = processInputFile(file);
            String outputFilename = getOutputFilename(
                    FilenameUtils.getBaseName(file.toString()));
            File outputFile = new File(outputDir, outputFilename); 
            writeModelToFile(outputModel, outputFile);
        }   
        
        return outputDir;
    }
    
    private Model processInputFile(File inputFile) {
        
        Model inputModel = readModelFromFile(inputFile);
        Model assertions = ModelFactory.createDefaultModel();
        Model retractions = ModelFactory.createDefaultModel();
        Map<String, Resource> labelToUriResource = new HashMap<String, Resource>();
        StmtIterator statements = inputModel.listStatements();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            convertBnodesToUris(statement, assertions, retractions, 
                    labelToUriResource);
        }
        inputModel.remove(retractions);
        inputModel.add(assertions);   
        return inputModel;
    }

    private void convertBnodesToUris(Statement statement, Model assertions,
            Model retractions, Map<String, Resource> labelToUriResource) {
        
        Resource subject = statement.getSubject();
        Property property = statement.getPredicate();
        RDFNode object = statement.getObject();
        if (!subject.isAnon() && !object.isAnon()) {
            return;
        }
        Resource newSubject = subject;
        RDFNode newObject = object;
        if (subject.isAnon()) {
            newSubject = createUriResourceForAnonNode(subject, 
                    labelToUriResource, assertions);
        }
        if (object.isAnon()) {
            newObject = createUriResourceForAnonNode(object, 
                    labelToUriResource, assertions);               
        }
        retractions.add(subject, property, object);
        // This handles cases where both subject and object are blank nodes.
        assertions.add(newSubject, property, newObject);
    }        
    
    
    private Resource createUriResourceForAnonNode(RDFNode rdfNode, 
            Map<String, Resource> labelToUriResource, Model assertions) {
        Node node = rdfNode.asNode();
        String label = node.getBlankNodeLabel();
        Resource uriResource;
        if (labelToUriResource.keySet().contains(label)) {
            uriResource = labelToUriResource.get(label);    
        } else {
            uriResource = assertions.createResource(convertLabelToUri(label));
            labelToUriResource.put(label, uriResource);
        }
        return uriResource;
    }
    
    private String convertLabelToUri(String label) {
        String localName = label.replaceAll("\\W", "");
        return localNamespace + localName;
    }

}
