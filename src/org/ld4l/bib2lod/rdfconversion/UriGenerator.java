package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;

public class UriGenerator extends RdfProcessor {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(UriGenerator.class);

    public UriGenerator(OntModel bfOntModelInf, String localNamespace, 
            String inputDir, String mainOutputDir) {
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);
    }

    @Override
    public String process() {        
        
        Instant processStart = Instant.now();
        LOGGER.info("START blank node conversion.");
        
        String outputDir = getOutputDir();

        File[] inputFiles = new File(inputDir).listFiles();
        int totalFileCount = inputFiles.length;
        
        int fileCount = 0;
        int timeFileCount = 0;
        Instant fileStart = Instant.now();

        // For consistent ordering. Helps locate errors if program exits
        // unexpectedly. Time to sort is miniscule (0.008 seconds on 34,540 
        // files).
        Arrays.sort(inputFiles);
        LOGGER.info("Sorted " + totalFileCount + " " + 
                Bib2LodStringUtils.simplePlural("file", totalFileCount) + ". "
                        + TimerUtils.getDuration(processStart)); 
        
        for ( File file : inputFiles ) {

            fileCount++;
            timeFileCount++;
            
            String filename = file.getName();
            
            LOGGER.info("Start unique URI generation in file " + filename
                    + " (file " + fileCount + " of " + totalFileCount  
                    + " input "
                    + Bib2LodStringUtils.simplePlural("file", totalFileCount)
                    + ").");
            
            Model outputModel = processInputFile(file);
            
            // Write out to same filename as input file
            String basename = FilenameUtils.getBaseName(file.toString());
            writeModelToFile(outputModel, basename);
            outputModel.close();
            
            LOGGER.info("End unique URI generation in file " + filename
                    + " (file " + fileCount + " of " + totalFileCount  
                    + " input "
                    + Bib2LodStringUtils.simplePlural("file", totalFileCount)
                    + ").");
            
            if (timeFileCount == TimerUtils.NUM_FILES_TO_TIME) {
                // TODO Define TIMER logging level between info and debug
                LOGGER.trace("Generated unique URIs in " + timeFileCount + " "
                        + Bib2LodStringUtils.simplePlural("file", timeFileCount)
                        + ". " + TimerUtils.getDuration(fileStart));
                timeFileCount = 0;
                fileStart = Instant.now();   
            }
        }   

        if (timeFileCount > 0) {
            // TODO Define TIMER logging level between info and debug
            LOGGER.trace("Generated unique URIs in " + timeFileCount + " "
                    + Bib2LodStringUtils.simplePlural("file", timeFileCount)
                    + ". " + TimerUtils.getDuration(fileStart));    
        } 
        
        LOGGER.info("END URI generation in total of " + totalFileCount 
                + " input "                                
                + Bib2LodStringUtils.simplePlural("file", totalFileCount)
                + ". " + TimerUtils.getDuration(processStart));
        
        return outputDir;
    }
    
    private Model processInputFile(File inputFile) {
        
        Model inputModel = readModelFromFile(inputFile);
        
        Model outputModel = ModelFactory.createDefaultModel();

        StmtIterator statements = inputModel.listStatements();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            Statement newStatement = generateUniqueUris(statement);                    
            outputModel.add(newStatement);
        }  
        
        return outputModel;
    }

    private Statement generateUniqueUris(Statement statement) {
            
        Resource subject = statement.getSubject();
        Resource newSubject = getUniqueResource(subject);
        
        RDFNode object = statement.getObject();
        RDFNode newObject = object.isResource() ?
                getUniqueResource(object.asResource()) :
                    object;
        
        return ResourceFactory.createStatement(
                newSubject, statement.getPredicate(), newObject);     
    }

    private Resource getUniqueResource(Resource resource) {
            
        String localname = resource.isAnon() ? 
                getLocalNameFromAnonNode(resource) :
                    getUniqueLocalNameForResource(resource);

        // Does it matter whether we create the resource from a ResourceFactory
        // rather than using outputModel.createResource()? Will the resource 
        // still  belong to the output model once the statement is added? Does 
        // it matter, since we're just outputting text in the end?
        return ResourceFactory.createResource(localNamespace + localname);

    }

    /* Replace bnodes with URI resources. Aside from general difficulties with
     * blank nodes, uniqueness is not guaranteed across input files, so Jena
     * may duplicate bnode ids across files when the entities should be 
     * distinct. Assigning fixed URIs solves this problem.
     */
    private String getLocalNameFromAnonNode(Resource resource) {
        return resource.asNode().getBlankNodeId().toString();
    }
    
    /*
     * This local name serves to dedupe the same resource across records,
     * based on type-specific identifying data.
     */
    private String getUniqueLocalNameForResource(Resource resource) {
            
        // TEMPORARY! 
        return resource.getLocalName();
        
//        // The resource type determines the query to get the identifying data.
//        Resource type = getResourceType(resource);
//        
//        // Get the identifying data and flatten to a string
//        String key = getUniqueKey(resource, type);
//        
//        // Hash the string to get the local name
//        String localname = getHashCode(key);
//              
//        return localname;
    }
    
    private Resource getResourceType(Resource resource) {
        
        Resource type = resource.hasProperty(RDF.type) ?
                getType(resource) :
                    inferTypeFromPredicates(resource);
        return type;
    }
        
    private Resource getType(Resource resource) {
        return null;
    }

    private Resource inferTypeFromPredicates(Resource resource) {           
        // TODO Auto-generated method stub
        return null;
    }
    
    /* Based on type of the resource, query the model for the values that will
     * comprise the identifying key, and concatenate into a single string.
     */
    private String getUniqueKey(Resource resource, Resource type) {
        // TODO Auto-generated method stub
        return null;
    }

    private String getHashCode(String key) {
        // TODO Returns a 32-bit hash code. Use a stronger 64-bit hash instead?
        return Integer.toString(key.hashCode());
    }
   
}