package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
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
import org.ld4l.bib2lod.rdfconversion.urigetter.BfAnnotationUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.BfAuthorityUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.BfCategoryUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.BfClassificationUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.BfHeldItemUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.BfIdentifierUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.BfInstanceUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.BfLanguageUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.BfTitleUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.BfTopicUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.BfWorkUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.MadsAuthorityUriGetter;
import org.ld4l.bib2lod.rdfconversion.urigetter.ResourceUriGetter;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;

public class UriGenerator extends RdfProcessor {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(UriGenerator.class);

    // Using LinkedHashMap in case order of processing becomes important
    private static final Map<Resource, BfType> TYPES_FOR_BF_ONT_CLASSES =
            BfType.typesForOntClasses();
    
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
                    + "}");
    
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
        
        // Maps a local URI generated by LC Bibframe converter to a unique
        // URI generated from uniquely identifying data. This will result in
        // reconciliation of entities across records within a single catalog.
        Map<String, String> uniqueUris = new HashMap<String, String>();

        List<Statement> statements = inputModel.listStatements().toList();
        for (Statement statement : statements) {
            Statement newStatement = generateUniqueUris(
                    statement, uniqueUris, outputModel);                    
            outputModel.add(newStatement);
        }  
        
        return outputModel;
    }

    private Statement generateUniqueUris(Statement statement,  
            Map<String, String> uniqueUris, Model outputModel) { 

        Resource subject = statement.getSubject();
        Resource newSubject = getUniqueUri(
                subject, uniqueUris, outputModel);
        
        RDFNode object = statement.getObject();
        RDFNode newObject = object.isLiteral() ? object :
                getUniqueUri(object.asResource(), uniqueUris,  
                        outputModel);
                            
        // Model.createStatement() may reuse an existing statement rather than
        // creating a new one.
        return outputModel.createStatement(
                newSubject, statement.getPredicate(), newObject);     
    }

    /*
     * The URI returned by this method serves to dedupe the same resource 
     * across records in a catalog, based on type-specific identifying data.
     */
    private Resource getUniqueUri(Resource resource, 
            Map<String, String> uniqueUris, Model outputModel) {

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
            
        } else if (! resource.getNameSpace().equals(localNamespace)) {
            // Don't modify URIs in an external namespace
            return resource;
        }
        
        String uri = resource.getURI();
        String uniqueUri;
 
        // If we've encountered this URI, return the stored value from the map.
        if (uniqueUris.containsKey(uri)) {
            uniqueUri = uniqueUris.get(uri);
            LOGGER.debug("Reusing unique URI " + uniqueUri 
                    + " previously generated for resource " 
                    + resource.getURI());

        } else {
            // Otherwise, compute a new value.
            uniqueUri = getNewUniqueUri(resource);
            LOGGER.debug("Generated new unique URI " + uniqueUri 
                    + " for resource " + resource.getURI());

            // Add to the map so the value can be reused for other resources in
            // the same record without having to recompute.
            uniqueUris.put(uri, uniqueUri);
        }
              
        // Model.createResource() may reuse an existing resource rather than
        // creating a new one.
        return outputModel.createResource(uniqueUri);
        
    }

    
    private String getNewUniqueUri(Resource resource) {
        
        // First, get the submodel containing all statements in which this
        // resource is either the subject or object.
        Model resourceSubModel = getResourceSubModel(resource);
        
        // RdfProcessor.printModel(resourceSubModel, 
        //         "Submodel for resource " + resource.getURI());
        
        // Point to the resource in the submodel so that Java methods on the
        // resource are querying the submodel rather than the full model.
        Resource newResource = 
                resourceSubModel.createResource(resource.getURI());        
        ResourceUriGetter uriGetter = getUriGetterByType(
                newResource, localNamespace);
        
        String uri = uriGetter.getUniqueUri();
        
        return uri;
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
    // **** TODO The type of the resource may determine the query. For example,
    // with instances we need info about identifiers, etc. So we need to 
    // get the type first, then get the resource sub model from the uri getter,
    // with the default in ResourceUriGetter and subclasses overriding where
    // needed. ****
    // Also, if the input files are small enough, there may not be a need to
    // get the submodel, rather than just querying the entire input model. It's
    // not clear whether there would be a performance difference and in which
    // direction. 
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

    private ResourceUriGetter getUriGetterByType(
            Resource resource, String localNamespace) {

        // Get the BfTypes for this resource
        List<Statement> typeStmts = resource.listProperties(RDF.type).toList();
        List<BfType> types = new ArrayList<BfType>();
        for (Statement stmt : typeStmts) {
            Resource ontClass = stmt.getResource();
            types.add(TYPES_FOR_BF_ONT_CLASSES.get(ontClass));
        }
        
        Class<?> uriGetterClass = null;

        if (types.contains(BfType.BF_TOPIC)) {
            uriGetterClass = BfTopicUriGetter.class;
        } else if (BfType.isAuthority(types)) {  
            uriGetterClass = BfAuthorityUriGetter.class;
        } else if (types.contains(BfType.BF_HELD_ITEM)) {
            uriGetterClass = BfHeldItemUriGetter.class;
        } else if (types.contains(BfType.BF_INSTANCE)) {
            uriGetterClass = BfInstanceUriGetter.class;
        } else if (types.contains(BfType.BF_WORK)) {
            uriGetterClass = BfWorkUriGetter.class;
        } else if (types.contains(BfType.BF_IDENTIFIER)) {
            uriGetterClass = BfIdentifierUriGetter.class;
        } else if (types.contains(BfType.BF_ANNOTATION)) {
            uriGetterClass = BfAnnotationUriGetter.class;
        } else if (types.contains(BfType.MADSRDF_AUTHORITY)) {
            uriGetterClass = MadsAuthorityUriGetter.class;
        } else if (types.contains(BfType.BF_CLASSIFICATION)) {
            uriGetterClass = BfClassificationUriGetter.class;
        } else if (types.contains(BfType.BF_LANGUAGE)) {
            uriGetterClass = BfLanguageUriGetter.class;
        } else if (types.contains(BfType.BF_TITLE)) {
            uriGetterClass = BfTitleUriGetter.class;          
        } else if (types.contains(BfType.BF_CATEGORY)) {
            uriGetterClass = BfCategoryUriGetter.class; 
        } else {
            uriGetterClass = ResourceUriGetter.class;
        }
        
        ResourceUriGetter uriGetter = null;
        if (uriGetterClass != null) {
            try {
                uriGetter = (ResourceUriGetter) uriGetterClass
                        .getConstructor(Resource.class, String.class)
                        .newInstance(resource, localNamespace);                        
            } catch (Exception e) {
                LOGGER.trace("Could not create UriGetter of type " 
                        + uriGetterClass.getName());
                e.printStackTrace();
            } 
        }
               
        return uriGetter; 
    }

}