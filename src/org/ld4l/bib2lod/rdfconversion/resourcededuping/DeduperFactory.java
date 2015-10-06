package org.ld4l.bib2lod.rdfconversion.resourcededuping;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class DeduperFactory {

    private static final Logger LOGGER = 
            LogManager.getLogger(DeduperFactory.class);
    
    private static final Map<OntType, Class<?>> TYPE_DEDUPERS =
            new HashMap<OntType, Class<?>>();
    static {
        TYPE_DEDUPERS.put(OntType.BF_EVENT, BfResourceDeduper.class);
        TYPE_DEDUPERS.put(OntType.BF_FAMILY, BfAgentDeduper.class);
        TYPE_DEDUPERS.put(OntType.BF_INSTANCE, BfInstanceDeduper.class);
        TYPE_DEDUPERS.put(OntType.BF_JURISDICTION,  BfAgentDeduper.class);
        TYPE_DEDUPERS.put(OntType.BF_MEETING,  BfAgentDeduper.class);
        TYPE_DEDUPERS.put(OntType.BF_ORGANIZATION,  BfAgentDeduper.class);        
        TYPE_DEDUPERS.put(OntType.BF_PERSON,  BfAgentDeduper.class);
        TYPE_DEDUPERS.put(OntType.BF_PLACE,  BfResourceDeduper.class);
        TYPE_DEDUPERS.put(OntType.BF_TOPIC,  BfTopicDeduper.class);
        TYPE_DEDUPERS.put(OntType.BF_WORK,  BfWorkDeduper.class);            
    }

    public static BfResourceDeduper createBfResourceDeduper(File inputFile) {

        // This case shouldn't occur since the file comes from an iteration on 
        // files in the input directory.
        if (inputFile == null) {
            LOGGER.debug("Can't create deduper: input file is null.");
            return null;
        }
        
        LOGGER.debug("Creating deduper for input file " + inputFile.getName());
        
        String basename = FilenameUtils.getBaseName(inputFile.toString());
        OntType type = OntType.getByFilename(basename);
        if (type == null) {
            LOGGER.debug("Can't create deduper: no type defined for file "
                   + basename);
            return null;
        }

        // Could use if-thens instead of reflection
        Class<?> deduperClass = TYPE_DEDUPERS.get(type);
        if (deduperClass == null) {
            LOGGER.debug("No deduper class defined for type " + type);
            return null;
        }
 
        try {
            return (BfResourceDeduper) deduperClass.getConstructor(
                    OntType.class).newInstance(type);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
   
    // If we want to turn the iteration around in ResourceDeduper and iterate
    // over types rather than files, implement this.
    public static BfResourceDeduper createBfResourceDeduper(
            OntType type, String inputDir) {
        // Get file basename from type (return null if null)
        // Get deduper class from TYPE_DEDUPERS (return null if null)
        // Read input file into model
        // Create the deduper, passing in the model
        return null;
    }
            

}
