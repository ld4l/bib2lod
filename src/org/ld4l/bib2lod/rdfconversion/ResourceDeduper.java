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
    private static final Map<BfType, Class<?>> DEDUPERS_BY_TYPE =
            new LinkedHashMap<BfType, Class<?>>();
    private static final List<BfType> TYPES_TO_DEDUPE = 
            new ArrayList<BfType>();
    static {
        DEDUPERS_BY_TYPE.put(BfType.BF_EVENT, BfResourceDeduper.class);
        DEDUPERS_BY_TYPE.put(BfType.BF_FAMILY, BfAuthorityDeduper.class);
        DEDUPERS_BY_TYPE.put(BfType.BF_HELD_ITEM, BfHeldItemDeduper.class);
        DEDUPERS_BY_TYPE.put(BfType.BF_INSTANCE, BfInstanceDeduper.class);
        DEDUPERS_BY_TYPE.put(BfType.BF_JURISDICTION,  BfAuthorityDeduper.class);
        DEDUPERS_BY_TYPE.put(BfType.BF_MEETING,  BfAuthorityDeduper.class);
        DEDUPERS_BY_TYPE.put(BfType.BF_ORGANIZATION,  BfAuthorityDeduper.class);        
        DEDUPERS_BY_TYPE.put(BfType.BF_PERSON,  BfAuthorityDeduper.class);
        DEDUPERS_BY_TYPE.put(BfType.BF_PLACE,  BfAuthorityDeduper.class);
        DEDUPERS_BY_TYPE.put(BfType.BF_TEMPORAL,  BfAuthorityDeduper.class);
        DEDUPERS_BY_TYPE.put(BfType.BF_TOPIC,  BfTopicDeduper.class);
        DEDUPERS_BY_TYPE.put(BfType.BF_WORK,  BfWorkDeduper.class); 
        
        // Don't just return the keySet directly, since (1) it's backed by the
        // map, and (2) since keySet() returns a Set, it's opaque to the caller
        // that the set is ordered (and backed by the map).
        Iterator<BfType> it = DEDUPERS_BY_TYPE.keySet().iterator();
        while (it.hasNext()) {
            TYPES_TO_DEDUPE.add(it.next());
        }
    }

    private static final String REMAINDER_FILENAME = "other";
    private static final String NEW_ASSERTIONS_FILENAME = "newAssertions";
    // private static final Format RDF_OUTPUT_FORMAT = Format.NTRIPLES;
    
    // Maps file basenames to deduper instances.
    private Map<String, BfResourceDeduper> dedupers;


    public ResourceDeduper(String inputDir, String mainOutputDir) {          
        super(inputDir, mainOutputDir);           
        dedupers = createDedupers();
    }
    
    public static String getNewAssertionsFilename() {
        return NEW_ASSERTIONS_FILENAME;
    }
    
    private Map<String, BfResourceDeduper> createDedupers() {
        
        Map<String, BfResourceDeduper> dedupers = 
                new HashMap<String, BfResourceDeduper>();
        
        for (Map.Entry<BfType, Class<?>> entry : 
                DEDUPERS_BY_TYPE.entrySet()) {
            BfType type = entry.getKey();
            String basename = type.filename();

            Class<?> deduperClass = entry.getValue();

            try {
                BfResourceDeduper deduper = (BfResourceDeduper) deduperClass
                        .getConstructor(BfType.class).newInstance(type);
                dedupers.put(basename,  deduper);      
            } catch (Exception e) {
                LOGGER.trace("No deduper created for type " + type);
                // TODO Auto-generated catch block
                e.printStackTrace();
            }              
        }
        
        return dedupers;
    }
    
    protected static List<BfType> getTypesToDedupe() {
        return TYPES_TO_DEDUPE;
    }
    
    protected static String getRemainderFilename() {
        return REMAINDER_FILENAME;
    }
    
    @Override
    public String process() {
        LOGGER.info("Start local URI deduping.");
        
        String outputDir = getOutputDir();    

        Map<String, String> uniqueUris = new HashMap<String, String>();

        File[] inputFiles = new File(inputDir).listFiles();
        
        getUniqueUris(inputFiles, uniqueUris);
        dedupeUris(inputFiles, uniqueUris, outputDir);

        LOGGER.info("End local URI deduping.");
        return outputDir;
    }


    private void getUniqueUris(File[] inputFiles, 
            Map<String, String> uniqueUris) {

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
//      for ( BfType type : TYPES_TO_DEDUPE ) {
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
//          newAssertions.add(statements);
//      }
//  }
        
        for ( File file : inputFiles ) {
            String filename = file.getName();
            LOGGER.trace("Deduping file " + filename);
            String basename = FilenameUtils.getBaseName(file.toString());
            BfResourceDeduper deduper = dedupers.get(basename);
            if (deduper == null) {
                LOGGER.trace("No deduper found for file " + filename);
                continue;
            }
            Model model = readModelFromFile(file);
            Map<String, String> dedupedUris = deduper.dedupe(model);
            if (dedupedUris != null) {
                uniqueUris.putAll(dedupedUris);
            }
            

            /*
             * If deduping has generated any new assertions (e.g., Instance
             * deduping asserts owl:sameAs between a local Instance URI and the
             * corresponding WorldCat URI), write these out to a file. Do per
             *  input file, so we don't have to hold all in memory. Add to a
             * separate model (file) rather than the original model so that
             * these statements don't receive any further processing.
             */
            writeNewAssertions(deduper.getNewAssertions());
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
                    LOGGER.trace("Replacing " + originalUri + " with " 
                            + newUri);                           
                    Resource resource = model.getResource(originalUri);
                    ResourceUtils.renameResource(resource, newUri);                          
                }
            }

            String basename = FilenameUtils.getBaseName(file.toString());
            
            /*
             * Second round of type-splitting: put each type into separate
             * file - i.e, separate Titles into separate file from Works and
             * Instances.
             * 
             * Could do as independent processing step after writing out each
             * model to a single file, but trying approach below for efficiency. 
             * Anyway, it's best to work through the dedupers because they are 
             * aware of what types were returned from their query.
             * splitIntoTypes(model, basename)
             * get the deduper
             * have it split into multiple models and return map of filenames
             * to models - or models to filenames, whichever is easier. Append 
             * each model to appropriate file.
             * 
             * Map<String, Model> models = writeModelsToFiles(models) or
             * Map<Model, String> models = writeModelsToFiles(models)
             * OR if the dedupers don't know anything about filenames, just 
             * have them return a map from Model to BfType (or the reverse),
             * and we transform that here to a map from Model to filename (or
             * the reverse.
             * 
             * Not seeing a need for this now. Possibly it could be a 
             * performance improvement: smaller files to read into converters,
             * don't have to loop through each type for each file, since we
             * know which type is represented in  each file. Save for later
             * consideration.
             *

             * 
             * BfResourceDeduper deduper = dedupers.get(basename);
             * if (deduper == null) {
             *    LOGGER.trace("No deduper found for file " + filename);
             *   appendModelToFile(model, basename);
             * } else {
             *    Map<BfType, Model> modelsByType = 
             *           deduper.getModelsByType(model);
             *   appendModelsToFiles(modelsByType);            
             * }
             * 
             */

            // Write out new model to file. Duplicate statements are 
            // automatically removed.
            LOGGER.trace("Writing model for file " + file.getName());
            writeModelToFile(model, basename);    
        }        
    }
   
    private void writeNewAssertions(Model newStatements) {
        if (!newStatements.isEmpty()) {
            appendModelToFile(newStatements, NEW_ASSERTIONS_FILENAME);
        }
    }
  
// Use for second round of type splitting. See comments above.
//    private void appendModelsToFiles(Map<BfType, Model> modelsByType) {
//        
//        for (Map.Entry<BfType, Model> entry : modelsByType.entrySet()) {
//            BfType type = entry.getKey();
//            Model model = entry.getValue();
//            String basename = type.filename();
//            appendModelToFile(model, basename);
//        }
//    }
    

}