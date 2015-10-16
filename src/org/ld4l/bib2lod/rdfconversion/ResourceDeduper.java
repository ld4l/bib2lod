package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.util.ResourceUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfAuthorityDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfHeldItemDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfInstanceDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfResourceDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfTopicDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfWorkDeduper;

// May need to be abstract - we only instantiate PersonDeduper, WorkDeduper, etc.
public class ResourceDeduper extends RdfProcessor {

    private static final Logger LOGGER = 
            LogManager.getLogger(ResourceDeduper.class);  
  
    /* Define types eligible for deduping.  Two kinds of types are excluded:
     * those where instances are not reused (e.g., Annotation, Title, HeldItem),
     * and those which are not likely to have independent significance (e.g.,
     * Provider is just a carrier for publication data - and more aptly named 
     * Provision). Some of these types are included in the triples for their
     * related type so that they can be used as a basis for deduping: e.g., 
     * Identifiers for Instances, Titles for Works and Instances). 
     */

    // Using LinkedHashMap in case order of processing becomes important
    private static final Map<OntType, Class<?>> RESOURCE_DEDUPERS =
            new LinkedHashMap<OntType, Class<?>>();
    private static final List<OntType> TYPES_TO_DEDUPE = 
            new ArrayList<OntType>();
    static {
        RESOURCE_DEDUPERS.put(OntType.BF_EVENT, BfResourceDeduper.class);
        RESOURCE_DEDUPERS.put(OntType.BF_FAMILY, BfAuthorityDeduper.class);
        RESOURCE_DEDUPERS.put(OntType.BF_HELD_ITEM, BfHeldItemDeduper.class);
        RESOURCE_DEDUPERS.put(OntType.BF_INSTANCE, BfInstanceDeduper.class);
        RESOURCE_DEDUPERS.put(OntType.BF_JURISDICTION,  BfAuthorityDeduper.class);
        RESOURCE_DEDUPERS.put(OntType.BF_MEETING,  BfAuthorityDeduper.class);
        RESOURCE_DEDUPERS.put(OntType.BF_ORGANIZATION,  BfAuthorityDeduper.class);        
        RESOURCE_DEDUPERS.put(OntType.BF_PERSON,  BfAuthorityDeduper.class);
        RESOURCE_DEDUPERS.put(OntType.BF_PLACE,  BfAuthorityDeduper.class);
        RESOURCE_DEDUPERS.put(OntType.BF_TOPIC,  BfTopicDeduper.class);
        RESOURCE_DEDUPERS.put(OntType.BF_WORK,  BfWorkDeduper.class); 
        
        // Don't just return the keySet directly, since (1) it's backed by the
        // map, and (2) since keySet() returns a Set, it's opaque to the caller
        // that the set is ordered (and backed by the map).
        Iterator<OntType> it = RESOURCE_DEDUPERS.keySet().iterator();
        while (it.hasNext()) {
            TYPES_TO_DEDUPE.add(it.next());
        }
    }
    
    // Maps file basenames to deduper instances.
    private Map<String, BfResourceDeduper> dedupers;

    private static final String REMAINDER_FILENAME = "other";
    private static final String NEW_STATEMENT_FILENAME = "newStatements";
    // private static final Format RDF_OUTPUT_FORMAT = Format.NTRIPLES;
    
    public ResourceDeduper(String inputDir, String mainOutputDir) {          
        super(inputDir, mainOutputDir);           
        dedupers = createDedupers();
    }
    
    private Map<String, BfResourceDeduper> createDedupers() {
        
        Map<String, BfResourceDeduper> dedupers = 
                new HashMap<String, BfResourceDeduper>();
        
        for (Map.Entry<OntType, Class<?>> entry : 
                RESOURCE_DEDUPERS.entrySet()) {
            OntType type = entry.getKey();
            String basename = type.filename();

            Class<?> deduperClass = entry.getValue();

            try {
                BfResourceDeduper deduper = (BfResourceDeduper) deduperClass
                        .getConstructor(OntType.class).newInstance(type);
                dedupers.put(basename,  deduper);      
            } catch (Exception e) {
                LOGGER.info("No deduper created for type " + type);
                // TODO Auto-generated catch block
                e.printStackTrace();
            }              
        }
        
        return dedupers;
    }
    
    protected static List<OntType> getTypesToDedupe() {
        return TYPES_TO_DEDUPE;
    }
    
    protected static String getRemainderFilename() {
        return REMAINDER_FILENAME;
    }
    
    @Override
    public String process() {
        LOGGER.info("Start process");
        
        String outputDir = getOutputDir();    

        Map<String, String> uniqueUris = new HashMap<String, String>();
        Model newStatements = ModelFactory.createDefaultModel();

        File[] inputFiles = new File(inputDir).listFiles();
        
        getUniqueUris(inputFiles, uniqueUris, newStatements);
        dedupeUris(inputFiles, uniqueUris, outputDir);
        writeNewStatements(newStatements);

        LOGGER.info("End process");
        return outputDir;
    }


    private void getUniqueUris(File[] inputFiles, 
            Map<String, String> uniqueUris, Model newStatements) {

        /* Can loop on input files or types to dedupe. In the former, send the
         * file to the factory; factory creates model from file, determines 
         * deduper type, sends model to deduper constructor to store in instance
         * field. In the latter, send the type to the factory; factory finds
         * file from type, creates model from file, and sends model to deduper
         * constructor to store in instance field. So results are the same,
         * but the latter has the potential of ordering deduping by type in
         * case there are dependencies. Iterating on files is easier, however,
         * so implementing that for now. 
         */
//      for ( OntType type : TYPES_TO_DEDUPE ) {
//      BfResourceDeduper deduper = 
//              DeduperFactory.createBfResourceDeduper(type, inputDir);
//      if (deduper == null) {
//          LOGGER.debug("No deduper found for type " + type);
//          continue;
//      }
//      Map<String, String> dedupedUris = deduper.dedupe();
//      if (dedupedUris != null) {
//          uniqueUris.putAll(dedupedUris);
//      }
//      
//      Model statements = deduper.getNewStatements();
//      if (statements != null) {
//          newStatements.add(statements);
//      }
//  }
        
        for ( File file : inputFiles ) {
            String filename = file.getName();
            LOGGER.info("Deduping file " + filename);
            String basename = FilenameUtils.getBaseName(file.toString());
            BfResourceDeduper deduper = dedupers.get(basename);
            if (deduper == null) {
                LOGGER.info("No deduper found for file " + filename);
                continue;
            }
            Model model = readModelFromFile(file);
            Map<String, String> dedupedUris = deduper.dedupe(model);
            if (dedupedUris != null) {
                uniqueUris.putAll(dedupedUris);
            }
            Model statements = deduper.getNewStatements();
            if (statements != null) {
                newStatements.add(statements);
            }
        }         
    }
    
    private void dedupeUris(File[] inputFiles, Map<String, String> uniqueUris,
            String outputDir) {

        for ( File file : inputFiles ) {

            // Rename each resource with the new URI specified in uniqueUris.
            // Renaming Resources rather than dumb string replacement avoids
            // problems of substrings, non-matching whitespace, etc. In 
            // addition, when working with a model, duplicate statements are
            // automatically removed rather than requiring a separate step.
            Model model = readModelFromFile(file);
            for (Map.Entry<String, String> entry : uniqueUris.entrySet()) {
                String originalUri = entry.getKey();
                String newUri = entry.getValue();
                // Instead of this test, we may want to simply leave entries 
                // where key and value are the same out of the map.
                if (! newUri.equals(originalUri)) {
                    LOGGER.info("Replacing " + originalUri + " with " + newUri);                           
                    Resource resource = model.getResource(originalUri);
                    ResourceUtils.renameResource(resource, newUri);                          
                }
            }

            String filename = file.getName();
            String basename = FilenameUtils.getBaseName(file.toString());

            // Could do as separate processing step after writing out each 
            // model to a single file, but trying this for efficiency. Anyway,
            // it's best to work through the dedupers because they are aware of
            // what types were returned from their query.
            // splitIntoTypes(model, basename)
            // get the deduper
            // have it split into multiple models and return map of filenames
            // to models - or models to filenames, whichever is easier. Append 
            // each model to appropriate file.

            // Map<String, Model> models = writeModelsToFiles(models) or
            // Map<Model, String> models = writeModelsToFiles(models)
            // OR if the dedupers don't know anything about filenames, just 
            // have them return a map from Model to OntType (or the reverse),
            // and we transform that here to a map from Model to filename (or
            // the reverse.
            
            BfResourceDeduper deduper = dedupers.get(basename);
            if (deduper == null) {
                LOGGER.info("No deduper found for file " + filename);
                appendModelToFile(model, basename);
            } else {
                Map<OntType, Model> modelsByType = 
                        deduper.getModelsByType(model);
                appendModelsToFiles(modelsByType);            
            }
        }        
    }
   
    private void writeNewStatements(Model newStatements) {
        // Assumes there aren't too many of these to hold all in memory. If too
        // many, append to file after each iteration through the loop.
        if (!newStatements.isEmpty()) {
            writeModelToFile(newStatements, NEW_STATEMENT_FILENAME);
        }
    }
    
    private void appendModelsToFiles(Map<OntType, Model> modelsByType) {
        
        for (Map.Entry<OntType, Model> entry : modelsByType.entrySet()) {
            OntType type = entry.getKey();
            Model model = entry.getValue();
            String basename = type.filename();
            appendModelToFile(model, basename);
        }
    }
    

}