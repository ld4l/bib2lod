package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.uniqueuris.BfAuthorityUriGenerator;
import org.ld4l.bib2lod.rdfconversion.uniqueuris.BfHeldItemUriGenerator;
import org.ld4l.bib2lod.rdfconversion.uniqueuris.BfInstanceUriGenerator;
import org.ld4l.bib2lod.rdfconversion.uniqueuris.BfResourceUriGenerator;
import org.ld4l.bib2lod.rdfconversion.uniqueuris.BfTopicUriGenerator;
import org.ld4l.bib2lod.rdfconversion.uniqueuris.BfWorkUriGenerator;
import org.ld4l.bib2lod.rdfconversion.uniqueuris.MadsAuthorityUriGenerator;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;

public class UriGenerator extends RdfProcessor {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(UriGenerator.class);

    private static final Map<Resource, BfType> BF_TYPES_FOR_ONT_CLASSES =
            BfType.typesForOntClasses();

    private static Map<BfType, BfResourceUriGenerator> uriGenerators =
            new HashMap<BfType, BfResourceUriGenerator>();
    
    private static final Map<BfType, Class<?>> URI_GENERATORS_BY_TYPE =
            new HashMap<BfType, Class<?>>();
    static {
        // Types that use BfResourceUriGenerator fall into two categories:
        // (1) They are unique resources, such as Providers and Titles, 
        // that are not reused. (2) BfResourceUriGenerator is a placeholder
        // until code for the specific type can be written.
        URI_GENERATORS_BY_TYPE.put(BfType.BF_ANNOTATION, 
                // BfAnnotationUriGenerator.class); 
                BfResourceUriGenerator.class);
        
        URI_GENERATORS_BY_TYPE.put(BfType.BF_AUTHORITY,
                BfAuthorityUriGenerator.class);

        URI_GENERATORS_BY_TYPE.put(BfType.BF_CATEGORY, 
                // BfCategoryUriGenerator.class);
                BfResourceUriGenerator.class);
        
        URI_GENERATORS_BY_TYPE.put(BfType.BF_CLASSIFICATION, 
                // BfClassificationUriGenerator.class);
                BfResourceUriGenerator.class);
        
        URI_GENERATORS_BY_TYPE.put(BfType.BF_HELD_ITEM, 
                BfHeldItemUriGenerator.class);
       
        URI_GENERATORS_BY_TYPE.put(BfType.BF_IDENTIFIER, 
                // BfIdentifierUriGenerator.class);
                BfResourceUriGenerator.class);
        
        URI_GENERATORS_BY_TYPE.put(
                BfType.BF_INSTANCE, BfInstanceUriGenerator.class);

        URI_GENERATORS_BY_TYPE.put(
                BfType.BF_RESOURCE, BfResourceUriGenerator.class);
        
        URI_GENERATORS_BY_TYPE.put(BfType.BF_TITLE, 
                // BfTitleUriGenerator.class);
                BfResourceUriGenerator.class);
        
        URI_GENERATORS_BY_TYPE.put(BfType.BF_TOPIC, BfTopicUriGenerator.class);

        URI_GENERATORS_BY_TYPE.put(BfType.BF_WORK,  BfWorkUriGenerator.class);
        
        URI_GENERATORS_BY_TYPE.put(BfType.MADSRDF_AUTHORITY, 
                MadsAuthorityUriGenerator.class);       
    }

    public UriGenerator(String localNamespace, String inputDir, 
            String mainOutputDir) {           
        super(localNamespace, inputDir, mainOutputDir);
        createUriGenerators();
    }

    private void createUriGenerators() {

        Map<Class<?>, BfResourceUriGenerator> instantiatedClasses = 
                new HashMap<Class<?>, BfResourceUriGenerator>();
        
        for (Map.Entry<BfType, Class<?>> entry : 
                URI_GENERATORS_BY_TYPE.entrySet()) {
            
            BfType bfType = entry.getKey();
            Class<?> generatorClass = entry.getValue();
            
            BfResourceUriGenerator uriGenerator;
            
            if (! instantiatedClasses.keySet().contains(generatorClass)) {
                
                try {
                    uriGenerator = (BfResourceUriGenerator) generatorClass                            
                            .getConstructor(String.class)
                            .newInstance(localNamespace);   
                    LOGGER.debug("Created converter for type " + bfType);
    
                } catch (Exception e) {
                    LOGGER.warn("Can't instantiate class " 
                            + generatorClass.getName());
                    e.printStackTrace();
                    return;
                }   
                
                instantiatedClasses.put(generatorClass, uriGenerator);
                
            } else {
                LOGGER.debug("Converter for class " + generatorClass 
                        + " already created.");
                uriGenerator = instantiatedClasses.get(generatorClass);
            }
            
            uriGenerators.put(bfType, uriGenerator);
        }
    }
    
    @Override
    public String process() {        
        
        Instant processStart = Instant.now();
        LOGGER.info("START unique URI generation.");

        String outputDir = getOutputDir();

        File[] inputFiles = new File(inputDir).listFiles();
        int totalFileCount = inputFiles.length;

        // For consistent ordering. Helps locate errors if program exits
        // unexpectedly. Time to sort is miniscule (0.008 seconds on 34,540 
        // files).
        Arrays.sort(inputFiles);

        int fileCount = 0;
        for ( File file : inputFiles ) {
            fileCount++;
            
            Instant fileStartTime = Instant.now();

            String filename = file.getName();
            
            LOGGER.info("Start unique URI generation in file " + filename
                    + " (file " + fileCount + " of "
                    + Bib2LodStringUtils.count(totalFileCount, "input file")
                    + ").");
            
            Model outputModel = convertFile(file);
            
            // Write out to same filename as input file
            String basename = FilenameUtils.getBaseName(file.toString());
            writeModelToFile(outputModel, basename);
            outputModel.close();
            
            LOGGER.info("End unique URI generation in file " + filename
                    + " (file " + fileCount + " of " 
                    + Bib2LodStringUtils.count(totalFileCount, "input file")
                    + " = " 
                    + TimerUtils.percent(fileCount, totalFileCount) + "%). "
                    + "Duration: " 
                    + TimerUtils.getDuration(fileStartTime) + ".");
        }   
       
        LOGGER.info("END URI generation in total of "       
                + Bib2LodStringUtils.count(totalFileCount, "input file")
                + ". Duration: " + TimerUtils.getDuration(processStart) + ".");
        
        return outputDir;
    }
    
    private Model convertFile(File inputFile) {
        
        Model inputModel = readModelFromFile(inputFile);    
        Model outputModel = ModelFactory.createDefaultModel();
        
        // Maps a local URI generated by LC Bibframe converter to a unique
        // URI generated from uniquely identifying data. This will result in
        // reconciliation of entities across records within a single catalog.
        Map<String, String> uniqueUris = new HashMap<String, String>();

        List<Statement> statements = inputModel.listStatements().toList();
        for (Statement statement : statements) {
            generateUniqueUris(statement, uniqueUris, outputModel);
        }  
        
        inputModel.close();

        return outputModel;
    }

    private void generateUniqueUris(Statement statement,  
            Map<String, String> uniqueUris, Model outputModel) { 

        Resource subject = statement.getSubject();
        String newSubjectUri = getUniqueUri(subject, uniqueUris, outputModel);
        Resource newSubject = outputModel.createResource(newSubjectUri);
        
        RDFNode object = statement.getObject();
        RDFNode newObject;
        if (object.isLiteral()) {
            newObject = object;
        } else {
            String newObjectUri = 
                    getUniqueUri(object.asResource(), uniqueUris, outputModel);
            newObject = outputModel.createResource(newObjectUri);
        }
                                           
        outputModel.add(
                newSubject, statement.getPredicate(), newObject);
    }

    /*
     * The URI returned by this method serves to dedupe the same resource 
     * across records in a catalog, based on type-specific identifying data.
     */
    private String getUniqueUri(Resource resource, 
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
            return resource.getURI();
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
            uniqueUri = getNewUniqueUri(resource, outputModel);
            LOGGER.debug("Generated new unique URI " + uniqueUri 
                    + " for resource " + uri);

            // Add to the map so the value can be reused for other resources in
            // the same record without having to recompute.
            uniqueUris.put(uri, uniqueUri);
        }
              
        return uniqueUri;
        
    }
 
    private String getNewUniqueUri(Resource resource, Model outputModel) {
        
        // **** Fix for URI collision bug across types:
        // Call a method here that returns the type needed to select the
        // uri generator - getTypeForGenerator(resource)   
        // Send that type to getUriGeneratorByType(type) - or do it here - all
        /// we will need then is a map; since the if statement goes into 
        // getTypeForGenerator()
        // Pass the type into getUniqueUri(resource, type)
        // getUniqueUri() must prepend the type to the unique local name, the
        // same as we do now with "authority". In fact, we won't need to 
        // use "authority" explicitly, it will just be the same as the others.
        // Keep instance URIs the same for now, so that usage data still works.
        
        BfResourceUriGenerator uriGenerator = getUriGeneratorByType(resource);

        String uniqueUri = uriGenerator.getUniqueUri(resource);
        
        // SIDE EFFECT
        // For instances, we need to get the local identifier, which the
        // LC converter embeds in the URI, before the URI is changed. Some
        // catalogs (Cornell) record this identifier in both 001 and 035, 
        // while others (Harvard, Stanford) only record it in 001. The
        // LC converter captures only the 035 value in triples, but it 
        // uses the 001 value in minting URIs.
        // NB Do this AFTER getting the unique URI, so the instance in the new
        // model has the new URI.
        if (uriGenerator instanceof BfInstanceUriGenerator) {
            Model model = ((BfInstanceUriGenerator) uriGenerator)   
                    // Could use uriGenerator.resource rather than passing 
                    // resource - doesn't the generator still have the resource?
                    .getLocalIdentifier(resource, uniqueUri);
            outputModel.add(model); 
            model.close();
        }
        
        return uniqueUri;
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
        // Don't use a randomly-generated temporary local name, else we can't
        // use the uniqueUris map to find previously processed resource and
        // reuse that URI.
        String tempUri = localNamespace + tempLocalName;
        bnode = ResourceUtils.renameResource(bnode, tempUri);
        // LOGGER.debug(resource.isURIResource() ? 
        //       "Resource is now a URI resource" : 
        //       "Resource is still a blank node");

        return bnode;
    }

    private BfResourceUriGenerator getUriGeneratorByType(Resource resource) {
            
        // Get the BfTypes for this resource
        List<Statement> typeStmts = resource.listProperties(RDF.type).toList();
        List<BfType> types = new ArrayList<BfType>();
        for (Statement stmt : typeStmts) {
            Resource type = stmt.getResource();
            types.add(BF_TYPES_FOR_ONT_CLASSES.get(type));
        }
        
        BfResourceUriGenerator uriGenerator;

        if (types.contains(BfType.BF_TOPIC)) {
            uriGenerator = uriGenerators.get(BfType.BF_TOPIC);
        } else if (BfType.isAuthority(types)) {  
            uriGenerator = uriGenerators.get(BfType.BF_AUTHORITY);
        } else if (types.contains(BfType.BF_HELD_ITEM)) {
            uriGenerator = uriGenerators.get(BfType.BF_HELD_ITEM);
        } else if (types.contains(BfType.BF_INSTANCE)) {
            uriGenerator = uriGenerators.get(BfType.BF_INSTANCE);
        } else if (types.contains(BfType.BF_WORK)) {
            uriGenerator = uriGenerators.get(BfType.BF_WORK);
        } else if (types.contains(BfType.BF_IDENTIFIER)) {
            uriGenerator = uriGenerators.get(BfType.BF_IDENTIFIER);
        } else if (types.contains(BfType.BF_ANNOTATION)) {
            uriGenerator = uriGenerators.get(BfType.BF_ANNOTATION);
        } else if (types.contains(BfType.MADSRDF_AUTHORITY)) {
            uriGenerator = uriGenerators.get(BfType.MADSRDF_AUTHORITY);
        } else if (types.contains(BfType.BF_CLASSIFICATION)) {
            uriGenerator = uriGenerators.get(BfType.BF_CLASSIFICATION);
        } else if (types.contains(BfType.BF_TITLE)) {
            uriGenerator = uriGenerators.get(BfType.BF_TITLE);          
        } else if (types.contains(BfType.BF_CATEGORY)) {
            uriGenerator = uriGenerators.get(BfType.BF_CATEGORY); 
        } else {
            uriGenerator = uriGenerators.get(BfType.BF_RESOURCE);
        }
        
        return uriGenerator; 
    }

}