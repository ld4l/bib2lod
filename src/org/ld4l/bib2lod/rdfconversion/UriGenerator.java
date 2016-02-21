package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.reconciliation.AuthorityReconciler;
import org.ld4l.bib2lod.rdfconversion.reconciliation.TopicAuthorityReconciler;
import org.ld4l.bib2lod.rdfconversion.uniquekey.UniqueKeyGetter;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.MurmurHash;
import org.ld4l.bib2lod.util.TimerUtils;

public class UriGenerator extends RdfProcessor {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(UriGenerator.class);
    
    private static ParameterizedSparqlString resourceSubModelPss = 
            new ParameterizedSparqlString(
                    "CONSTRUCT { ?s ?p1 ?o1 . "
                    + " ?o1 ?p2 ?o2 . "                
                    + "} WHERE {  "                                                      
                    + "?s ?p1 ?o1 . "
                    + "OPTIONAL { "
                    + "?s " 
                    + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?o1 . "
                    + "?o1 a " + BfType.MADSRDF_AUTHORITY.sparqlUri() + " . "
                    + "?o1 ?p2 ?o2 . "
                    + "} FILTER (?s = ?resource || ?o1 = ?resource) "
                    + " }");
    
    public UriGenerator(String localNamespace, String inputDir, 
            String mainOutputDir) {           
        super(localNamespace, inputDir, mainOutputDir);
    }

    @Override
    public String process() {        
        
        Instant processStart = Instant.now();
        LOGGER.info("START unique URI generation.");
        // LOGGER.debug(resourceSubModelPss.toString());
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
        
        LOGGER.debug("Processing input file " + inputFile.getName());
        Model inputModel = readModelFromFile(inputFile);    
        Model outputModel = ModelFactory.createDefaultModel();
        
        // Create an inference model over the Bibframe ontology and the input
        // data. Bibframe only contains RDFS-level axioms, and that is all we
        // need here (subclass, subproperty, and domain/range inferencing).
        // InfModel infModel = ModelFactory.createInfModel(
        //     ReasonerRegistry.getRDFSReasoner(), bfOntModel, inputModel);       
        // RdfProcessor.printModel(infModel, "infModel:");
        
        Map<String, String> uniqueLocalNames = new HashMap<String, String>();

        List<Statement> statements = inputModel.listStatements().toList();
        for (Statement statement : statements) {
            Statement newStatement = generateUniqueUris(
                    statement, uniqueLocalNames, outputModel);                    
            outputModel.add(newStatement);
        }  
        
        return outputModel;
    }

    private Statement generateUniqueUris(Statement statement,  
            Map<String, String> uniqueLocalNames, Model outputModel) { 

        Resource subject = statement.getSubject();
        Resource newSubject = getUniqueUri(
                subject, uniqueLocalNames, outputModel);
        
        RDFNode object = statement.getObject();
        RDFNode newObject = object.isLiteral() ? object :
                getUniqueUri(object.asResource(), uniqueLocalNames,  
                        outputModel);
                            
        // Model.createStatement() may reuse an existing statement rather than
        // creating a new one.
        return outputModel.createStatement(
                newSubject, statement.getPredicate(), newObject);     
    }

    private Resource getUniqueUri(Resource resource, 
            Map<String, String> uniqueLocalNames, Model outputModel) {
        
        if (resource.hasProperty(RDF.type, BfType.BF_TOPIC.ontClass())) {
            AuthorityReconciler reconciler = 
                    new TopicAuthorityReconciler(resource);
            String uri = reconciler.reconcile();
            if (uri != null) {
                return outputModel.createResource(uri);
            }
        }
                
        String localName;
        
        // Blank nodes (titles, authorities, etc.) also need to be deduped 
        // rather than assigned a random local name.
        if (resource.isAnon() || 
                resource.getNameSpace().equals(localNamespace) ) {
            localName = getUniqueLocalName(
                    resource, uniqueLocalNames);
            
        } else {
            // Don't dedupe URIs in an external namespace
            return resource;
        }

        // Model.createResource() may reuse an existing resource rather than
        // creating a new one.
        return outputModel.createResource(localNamespace + localName);
    }
    
    /*
     * This local name serves to dedupe the same resource across records,
     * based on type-specific identifying data.
     */
    private String getUniqueLocalName(Resource resource, 
            Map<String, String> uniqueLocalNames) {
                
        // boolean isBnode = false;
        
        String localname;

        // Assign a temporary URI to a blank node, so that remaining processing
        // can be the same as for a URI resource. This method renames the
        // resource throughout the model (here, the input model).
        if (resource.isAnon()) {
            resource = assignTempLocalNameToBnode(resource);
            // Two reasons we may want this: (1) Prefix a string to the unique
            // local name to avoid collisions with related resources; for 
            // example, a madsrdf:Authority and its related resource are 
            // initially assigned the same unique key, since they are derived 
            // from identical data. Currently we prefix a string only for
            // authorities, but possibly we need to do this for any blank nodes.
            // (2) Pass to getUniqueKey() since this information may be used to
            // determine how to get the unique key. 
            // isBnode = true; 
     
        } 
            
        localname = resource.getLocalName();
        // LOGGER.debug("Got local name " + localname 
        //         + " for URI resource");
        
        String uniqueLocalName;
        // If we've encountered this local name before, return the value from
        // the map.
        if (uniqueLocalNames.containsKey(localname)) {
            LOGGER.debug("Reusing unique local name previously generated "
                    + "for resource " + resource.getURI());
            uniqueLocalName = uniqueLocalNames.get(localname);
        } else {
            // Otherwise, compute a new value.
            LOGGER.debug("Generating new unique local name for resource "
                    + resource.getURI());
            uniqueLocalName = getNewUniqueLocalName(resource);
            // Add to the map so the value can be reused for other statements in
            // the same record without having to recompute.
            uniqueLocalNames.put(localname, uniqueLocalName);
        }
        
        return uniqueLocalName;        
    }
    
    private String getNewUniqueLocalName(Resource resource) {
        
        // First, get the submodel containing all statements in which this
        // resource is either the subject or object.
        Model resourceSubModel = getResourceSubModel(resource);
        
        // Point to the resource in the submodel so that Java methods on the
        // resource are querying the submodel rather than the full model.
        Resource newResource = 
                resourceSubModel.createResource(resource.getURI());
        
        // Get a unique key based on relevant attributes of the resource. 
        UniqueKeyGetter keyGetter = new UniqueKeyGetter(newResource);
        String key = keyGetter.getUniqueKey();

        // if (isBnode) {
        //     key = "bnode" + key;
        // }

        // Hash the key, and prefix "n" so the local name doesn't start with a
        // digit.
        return "n" + getHashCode(key);
    }

    /* 
     * Assign a temporary URI to a blank node, so further processing can be the
     * same as for a URI resource.
     */
    private Resource assignTempLocalNameToBnode(Resource bnode) {
        
        // Same as: resource.asNode().getBlankNodeId().toString()
        // and: resource.getId().getBlankNodeId().toString()
        String tempLocalName = "n" + bnode.getId().toString();
        
        // LOGGER.debug("Got bnode id " + localname);
        String tempUri = localNamespace + tempLocalName;

        bnode = ResourceUtils.renameResource(bnode, tempUri);
        // LOGGER.debug("Renamed blank node " + tempLocalName + " to " 
        //       + bnode.getURI());
        // LOGGER.debug(resource.isURIResource() ? 
        //       "Resource is now a URI resource" : 
        //       "Resource is still a blank node");

        return bnode;
    }
    
    /*
     * Get the submodel of the input model consisting of statements in which 
     * this resource is either the subject or object.
     */
    private Model getResourceSubModel(Resource resource) {

        LOGGER.debug("Getting resource submodel for " + resource.getURI());
        resourceSubModelPss.setIri("resource", resource.getURI());
        
        Query query = resourceSubModelPss.asQuery();
        QueryExecution qexec = QueryExecutionFactory.create(
                query, resource.getModel());
        Model resourceSubModel = qexec.execConstruct();
        
        return resourceSubModel;  

        /*
         * Experiments with inference models. 
         * 
         * Bibframe only contains RDFS-level axioms, and that is all we need
         * here (subclass, subproperty, and domain/range inferencing).
         * 
         * In the first scenario, we create an inference model for the entire 
         * file, pass it here, and query it to build the submodel. This results
         * in a small submodel that contains only the assertions and inferences
         * related to the resource. This is the approach we would want.
         * 
         * In the second scenario, we build the construct model as above by 
         * querying the input model, then construct an inference model from it.
         * This results in a large submodel that contains the ontology and all
         * the inferences on it, in addition to assertions and inferences 
         * related to the resource.
         * 
         * For example, with an input model for one resource containing 7
         * assertions, the final submodel in scenario 1 is 9 statements, while
         * the final submodel in scenario 2 is a few thousand statements.
         * 
         * NB Sizes of inference models reported by InfModel.size() are not 
         * accurate. It is necessary to look at the actual statements as output 
         * by RdfProcessor.printModel().
         * 
         * Inferences from the Bibframe ontology are not always reliable. For
         * example, consider:
         <rdf:Property rdf:about="http://bibframe.org/vocab/subject">
             <rdfs:domain rdf:resource="http://bibframe.org/vocab/Work"/>
             <rdfs:label>Subject</rdfs:label>
             <rdfs:range rdf:resource="http://bibframe.org/vocab/Authority"/>
             <rdfs:range rdf:resource="http://bibframe.org/vocab/Work"/>
             <rdfs:comment>Subject term(s) describing a resource.</rdfs:comment>
         </rdf:Property>
         * Now every object of bf:subject is inferred to be a Work. The object 
         * of the range assertion should be the UNION of bf:Authority and 
         * bf:Work, but of course since Bibframe is not an OWL ontology it 
         * cannot use unionOf. 
         *
         LOGGER.debug("Submodel with no inferencing: ");
         LOGGER.debug("Submodel size: " + resourceSubModel.size());
         RdfProcessor.printModel(resourceSubModel, "Resource submodel: ");
        
         QueryExecution qexecInf = QueryExecutionFactory.create(
                 query, infModel);
         Model resourceSubModel1 = qexecInf.execConstruct();  
         LOGGER.debug("Submodel built from querying the inference model that "
                 + "is based on the entire file:");
         LOGGER.debug("Submodel size: " + resourceSubModel1.size());
         RdfProcessor.printModel(resourceSubModel1, "Resource submodel:");

         InfModel resourceSubModel2 = ModelFactory.createInfModel(
                 ReasonerRegistry.getRDFSReasoner(), bfOntModel, 
                 resourceSubModel);
         LOGGER.debug("Submodel built from creating an inference model based on "
                 + "the submodel built from querying the input model:");
         LOGGER.debug("Submodel size: " + resourceSubModel2.size());
         RdfProcessor.printModel(resourceSubModel2, "Resource submodel: ");
         */

    }

    private String getHashCode(String key) {
        // long hash64 = Crc64.checksum(key);
        // long hash64 = Crc64Mod.checksum(key);
        // See https://en.wikipedia.org/wiki/MurmurHash on various MurmurHash
        // algorithms and 
        // http://blog.reverberate.org/2012/01/state-of-hash-functions-2012.html
        // for improved algorithms.There are variants of Murmur Hash optimized 
        // for a 64-bit architecture. 
        long hash64 = MurmurHash.hash64(key);
        return Long.toHexString(hash64);        
    }
}