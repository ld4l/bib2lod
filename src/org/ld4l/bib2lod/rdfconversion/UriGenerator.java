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
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.MurmurHash;
import org.ld4l.bib2lod.util.NacoNormalizer;
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
                    + "} FILTER (?s = ?uri || ?o1 = ?uri) "
                    + " }");
                                                        

    private static ParameterizedSparqlString authLabelPss = 
            new ParameterizedSparqlString(
                  "SELECT ?authLabel WHERE { "
                  + "?s " + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " "
                  + "?auth . "
                  + "?auth a " 
                  + BfType.MADSRDF_AUTHORITY.sparqlUri() + " . "
                  + "?auth "
                  + BfProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() + " "                                  
                  + "?authLabel . "
                  + "} ");
            
    
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
            
        String localName;
        
        // Blank nodes (titles, authorities, etc.) also need to be deduped 
        // rather than assigned a random local name.
        if (resource.isAnon() || 
                resource.getNameSpace().equals(localNamespace) ) {
            localName = getUniqueLocalNameForResource(
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
    private String getUniqueLocalNameForResource(Resource resource, 
            Map<String, String> uniqueLocalNames) {
                
        // boolean isBnode = false;
        
        String localname;
        
        if (resource.isAnon()) {
            // Same as: resource.asNode().getBlankNodeId().toString()
            // and: resource.asNode().getBlankNodeId().toString()
            localname = resource.getId().toString();
            // LOGGER.debug("Got bnode id " + localname);
            String tempUri = localNamespace + "n" + localname;
            // Assign a temporary URI to the blank node, so that remaining 
            // processing can be the same as for a URI resource. This method
            // renames the resource throughout the model (here, the input 
            // model).
            resource = ResourceUtils.renameResource(resource, tempUri);
            // LOGGER.debug("Renamed blank node " + localname + " to " 
            //       + resource.getURI());
            // LOGGER.debug(resource.isURIResource() ? 
            //       "Resource is now a URI resource" : 
            //       "Resource is still a blank node");
            
            // Two reasons we may want this: (1) Prefix a string to the unique
            // local name to avoid collisions with related resources; for 
            // example, a madsrdf:Authority and its related resource are 
            // initially assigned the same unique key, since they are derived 
            // from identical data. Currently we prefix a string only for
            // authorities, but possibly we need to do this for any blank nodes.
            // (2) Pass to getUniqueKey() since this information may be used to
            // determine how to get the unique key. 
            // isBnode = true;
      
        } else {
            localname = resource.getLocalName();
            // LOGGER.debug("Got local name " + localname + " for URI resource");
        }
        
        // If we've encountered this local name before, return the hashed value
        // from the map.
        if (uniqueLocalNames.containsKey(localname)) {
            return uniqueLocalNames.get(localname);
        }
        
        // Otherwise, compute a new hashed value.
        
        // First, get the submodel containing all statements in which this
        // resource is either the subject or object.
        Model resourceSubModel = getResourceSubModel(resource);
        
        // Create a new resource in the submodel so that Java methods on the
        // resource are querying the submodel rather than the full model.
        Resource newResource = 
                resourceSubModel.createResource(resource.getURI());
        
        String key = getUniqueKey(newResource);  
        
        // if (isBnode) {
        //     key = "bnode" + key;
        // }

        String uniqueLocalName = "n" + getHashCode(key);
        
        uniqueLocalNames.put(localname, uniqueLocalName);
        
        return uniqueLocalName;
    }
    
    /*
     * Get the submodel of the input model consisting of statements in which 
     * this resource is either the subject or object.
     */
    
//    1. Get construct submodel for resource, make into an inference model
//    2. Get inference model for file, build construct model by querying it
    
    private Model getResourceSubModel(Resource resource) {

        resourceSubModelPss.setIri("uri", resource.getURI());
        
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
                 ReasonerRegistry.getRDFSReasoner(), bfOntModel, resourceSubModel);
         LOGGER.debug("Submodel built from creating an inference model based on "
                 + "the submodel built from querying the input model:");
         LOGGER.debug("Submodel size: " + resourceSubModel2.size());
         RdfProcessor.printModel(resourceSubModel2, "Resource submodel: ");
         */

    }
    
    /* Based on statements in which the resource is either a subject or object
     * get the identifying key.
     */
    private String getUniqueKey(Resource resource) {
        
        //**** TODO need to do some type-specific stuff first. E.g., two topics
        // will not be distinct just on the basis of the authoritativeLabel.
        // Need to come before the authority treatment because topics are
        // also authorities. Need to do the FAST stuff here.
        
        String key = null;
        
        if (resource.hasProperty(RDF.type, 
                BfType.MADSRDF_AUTHORITY.ontClass())) {
            return getMadsAuthorityKey(resource);
        }
        
        // Use authorizedAccessPoint where it exists.
        key = getKeyFromAuthorizedAccessPoint(resource);
        
        if (key == null) {
            // Not sure if we ever get an authority label without an 
            // authorized access point, so this may be redundant.
            key = getKeyFromAuthoritativeLabel(resource);
        }

        // TODO: QUESTION: should we make an inferencing model so that we can
        // infer types from subtypes, domain/range, etc, rather than having to
        // code these inferences. Figure out how to do it. Should the ontology
        // be inferencing? - we're not reasoning on the ontology, but on the
        // data on the basis of the ontology. See tests in javatest project.
        // make inferencing model here, pass to next 2 methods.
        
        if (key == null) {
            key = getKeyFromType(resource);
        }
        
        if (key == null) {
            key = getKeyFromPredicate(resource);
        }

        if (key == null) {
            key = getKeyFromBfLabel(resource);
        }
        
        if (key == null) {
            // TEMPORARY! - or will this be the fallback?
            key = resource.getLocalName();
        }
       
        return key;
    }

    /*
     * Get key for a madsrdf:Authority from the madsrdf:authoritativeLabel
     * property. Note that other resources that get their key from the
     * property chain bf:hasAuthority/madsrdf:authoritativeLabel are handled
     * in getKeyFromAuthoritativeLabel.
     */
    private String getMadsAuthorityKey(Resource resource) {
        
        String authoritativeLabel = null;

        List<Statement> statements = resource.listProperties(
                BfProperty.MADSRDF_AUTHORITATIVE_LABEL.property()).toList();

        for (Statement statement : statements) {
            RDFNode object = statement.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                authoritativeLabel = literal.getLexicalForm();
                break;
            }
        }
    
        // Prefix an additional string to the label, else the local name of 
        // the Authority will collide with that of the related resource, since
        // generally the madsrdf:authorizedLabel of the Authority and the
        // bf:authorizedAccessPoint of the related resource are identical.
        // *** TODO Are there other blank nodes that also need to be 
        // distinguished from the related resources?
        authoritativeLabel = "authority "  
                + NacoNormalizer.normalize(authoritativeLabel);
        LOGGER.debug("Got authoritativeLabel key " + authoritativeLabel
                + " for resource " + resource.getURI());
        
        return authoritativeLabel;
    }

    private String getKeyFromAuthorizedAccessPoint(Resource resource) {
        
        String authAccessPoint = null;

        List<Statement> statements = resource.listProperties(
                BfProperty.BF_AUTHORIZED_ACCESS_POINT.property()).toList();

        for (Statement statement : statements) {
            RDFNode object = statement.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                authAccessPoint = literal.getLexicalForm();
                String lang = literal.getLanguage();
                if (lang.equals("x-bf-hash")) {
                    LOGGER.debug("Got authAccessPoint key " + authAccessPoint
                            + " for resource " + resource.getURI());
                    // Don't look any further, and no need to normalize the
                    // string.
                    return authAccessPoint;
                } else {  
                    // If there is more than one (which there shouldn't be), 
                    // we'll get the first one, but doesn't matter if we have 
                    // no selection criteria. 
                    break;
                }
            }
        }
    
        authAccessPoint = NacoNormalizer.normalize(authAccessPoint);
        LOGGER.debug("Got authAccessPoint key " + authAccessPoint
                + " for resource " + resource.getURI());
        
        return authAccessPoint;
    }
    
    /*
     * Get resource key from the associated madsrdf:Authority authorizedLabel.
     * Getting the key for the madsrdf:Authority itself is done in 
     * getMadsAuthorityKey().
     */
    private String getKeyFromAuthoritativeLabel(Resource resource) {
        
        String authorizedLabel = null;

        // Easier to do this as a SPARQL query since we're jumping over the 
        // intermediate madsrdf:Authority node.
        authLabelPss.setIri("s", resource.getURI());
        LOGGER.debug(authLabelPss.toString());
        Query query = authLabelPss.asQuery();

        QueryExecution qexec = 
                QueryExecutionFactory.create(query, resource.getModel());
        ResultSet results = qexec.execSelect();
        
        // If there is more than one (which there shouldn't be), we'll get the
        // first one, but doesn't matter if we have no selection criteria. 
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            RDFNode node = soln.get("authLabel");
            if (node.isLiteral()) {
                Literal literal = node.asLiteral();
                authorizedLabel = literal.getLexicalForm();
                break;
            }
        }
    
        authorizedLabel = NacoNormalizer.normalize(authorizedLabel);
        LOGGER.debug("Got authorizedLabel key " + authorizedLabel
                + " from madsrdf:Authority for resource " + resource.getURI());
        
        return authorizedLabel;
    }
    
    private String getKeyFromBfLabel(Resource resource) {
        
        String bfLabel = null;

        List<Statement> statements = resource.listProperties(
                BfProperty.BF_LABEL.property()).toList();

        // If there is more than one (which there shouldn't be), we'll get the
        // first one, but doesn't matter if we have no selection criteria. 
        for (Statement statement : statements) {
            RDFNode object = statement.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                bfLabel = literal.getLexicalForm();
                break;
            }
        }
    
        bfLabel = NacoNormalizer.normalize(bfLabel);
        LOGGER.debug("Got bf:label key " + bfLabel + " for resource " 
                + resource.getURI());
                
        return bfLabel;
    }
    

    private String getKeyFromType(Resource resource) {
        return null;
    }
    
    private String getKeyFromPredicate(Resource resource) {
        return null;
    }

    private Resource getResourceType(
            Resource resource, Model resourceSubModel) {
        
        Resource type = resource.hasProperty(RDF.type) ?
                getPrimaryType(resource, resourceSubModel) :
                    inferTypeFromPredicates(resource, resourceSubModel);
                
        // TODO If the type had to be inferred, add it to the outputModel so 
        // that it doesn't have to be re-inferred during Bibframe conversion?
        // Will we need that statement? Is it costly to compute the inference?
        return type;
    }
        
    private Resource getPrimaryType(Resource resource, Model resourceSubModel) {
        
        
        return null;
    }

    private Resource inferTypeFromPredicates(
            Resource resource, Model resourceSubModel) {           
        // TODO Auto-generated method stub
        return null;
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