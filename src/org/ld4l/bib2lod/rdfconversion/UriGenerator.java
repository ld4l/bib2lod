package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
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
    
    private static ParameterizedSparqlString pss = 
            new ParameterizedSparqlString(
                    "CONSTRUCT { ?s ?p ?o } WHERE { " 
                    + "?s ?p ?o "
                    + "FILTER (?s = ?uri || ?o = ?uri)"
                    + " }");
    
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
        Map<String, String> uniqueLocalNames = new HashMap<String, String>();

        StmtIterator statements = inputModel.listStatements();
        while (statements.hasNext()) {
            Statement statement = statements.next();
            Statement newStatement = generateUniqueUris(
                    statement, uniqueLocalNames, outputModel);                    
            outputModel.add(newStatement);
        }  
        
        return outputModel;
    }

    private Statement generateUniqueUris(Statement statement, 
            Map<String, String> uniqueLocalNames, Model outputModel) {
            
        Resource subject = statement.getSubject();
        Resource newSubject = getUniqueResource(
                subject, uniqueLocalNames, outputModel);
        
        RDFNode object = statement.getObject();
        RDFNode newObject = object.isLiteral() ? object :
                getUniqueResource(
                        object.asResource(), uniqueLocalNames, outputModel);
     
        // Model.createStatement() may reuse an existing statement rather than
        // creating a new one.
        return outputModel.createStatement(
                newSubject, statement.getPredicate(), newObject);     
    }

    private Resource getUniqueResource(Resource resource, 
            Map<String, String> uniqueLocalNames, Model outputModel) {
            
        String localName;
        
        if (resource.isAnon()) {
            localName = getLocalNameForAnonNode(resource);
            
        } else if (! resource.getNameSpace().equals(localNamespace)) {
            return resource;

        } else {
            localName = getUniqueLocalNameForResource(
                    resource, uniqueLocalNames);
        }
        
        // Model.createResource() may reuse an existing resource rather than
        // creating a new one.
        return outputModel.createResource(localNamespace + localName);

    }

    /* Replace bnodes with URI resources. Aside from general difficulties with
     * blank nodes, uniqueness is not guaranteed across input files, so Jena
     * may duplicate bnode ids across files when the entities should be 
     * distinct. Assigning fixed URIs solves this problem.
     */
    private String getLocalNameForAnonNode(Resource bnode) {
        return bnode.asNode().getBlankNodeId().toString();
    }
    
    /*
     * This local name serves to dedupe the same resource across records,
     * based on type-specific identifying data.
     */
    private String getUniqueLocalNameForResource(
            Resource resource, Map<String, String> uniqueLocalNames) {
                
        String localName = resource.getLocalName();
        
        // If we've encountered this local name before, return the hashed value
        // from the map.
        if (uniqueLocalNames.containsKey(localName)) {
            return uniqueLocalNames.get(localName);
        }
        
        // Otherwise, compute a new hashed value.
        
        // First, get the submodel containing all statements in which this
        // resource is either the subject or object.
        Model resourceSubModel = getResourceSubModel(resource);
          
        // The resource type determines the query to get the identifying data.
        Resource type = getResourceType(resource, resourceSubModel);
//        
//        // Get the identifying data and flatten to a string
//        String key = getUniqueKey(resource, type);
//        
//        // Hash the string to get the new local name
//        String uniqueLocalName = getHashCode(key);
//              
      
        // TEMPORARY!  
        // Append "unique" just so we know this is working.
        String uniqueLocalName = localName + "unique";
        
        uniqueLocalNames.put(localName, uniqueLocalName);
        return uniqueLocalName;
    }
    
    
    /*
     * Get the submodel of the input model consisting of statements in which 
     * this resource is either the subject or object.
     */
    private Model getResourceSubModel(Resource resource) {
        
        Model inputModel = resource.getModel();

        pss.setIri("uri", resource.getURI());
        
        Query query = pss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.create(
                query, inputModel);
        Model constructModel = qexec.execConstruct();
        
        return constructModel;
    }
    

    private Resource getResourceType(Resource resource, Model model) {
        
        Resource type = resource.hasProperty(RDF.type) ?
                getType(resource, model) :
                    inferTypeFromPredicates(resource, model);
                
        // TODO If the type had to be inferred, add it to the outputModel so 
        // that it doesn't have to be re-inferred during Bibframe conversion.
        return type;
    }
        
    private Resource getType(Resource resource, Model model) {
        return null;
    }

    private Resource inferTypeFromPredicates(Resource resource, Model model) {           
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