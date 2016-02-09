package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;

public class UriGenerator extends RdfProcessor {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(UriGenerator.class);

    public UriGenerator(String localNamespace, String inputDir,
            String mainOutputDir) {
        super(localNamespace, inputDir, mainOutputDir);
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
        
        LOGGER.info("END URI generation in all " + totalFileCount + " input "                                
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
            Model newModel = generateUniqueUris(statement, inputModel);                    
            outputModel.add(newModel);
        }  
        
        return outputModel;
    }

    private Model generateUniqueUris(Statement statement, Model inputModel) {
                          
        Model outputModel = ModelFactory.createDefaultModel();
        Resource subject = statement.getSubject();
        Resource newSubject = getUniqueUri(subject, inputModel);
        
        RDFNode object = statement.getObject();
        RDFNode newObject;
        if (object.isResource()) {
            newObject = getUniqueUri(object.asResource(), inputModel);
        } else {
            // Literal object
            newObject = object;
        }
        
        outputModel.add(newSubject, statement.getPredicate(), newObject);
        return outputModel;
        
    }

    private Resource getUniqueUri(Resource resource, Model inputModel) {
            
        Resource newResource;
        
        if (resource.isAnon()) {
            newResource = createUriResourceFromAnonNode(resource);
        } else {
            // newResource = createUniqueUri(resource, inputModel);
            // Temporary: 
            newResource = resource;
        }
        
        return newResource;
    }

    /* Replace bnodes with URI resources. Aside from general difficulties with
     * blank nodes, uniqueness is not guaranteed across input files, so Jena
     * may duplicate bnode ids across files when the entities should be 
     * distinct. Assigning fixed URIs solves this problem.
     */
    private Resource createUniqueUri(Resource resource, Model inputModel) {
        // TODO Auto-generated method stub
        return null;
    }

    private Resource createUriResourceFromAnonNode(Resource resource) {
        
        Node node = resource.asNode();
        String id = node.getBlankNodeId().toString();
        return ResourceFactory.createResource(localNamespace + id); 
    }
    
}