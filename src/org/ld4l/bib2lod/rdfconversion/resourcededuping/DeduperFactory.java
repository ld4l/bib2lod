package org.ld4l.bib2lod.rdfconversion.resourcededuping;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfType;

/* TODO Much of this code is repeated in DeduperFactory. Combine if possible.
 * Maybe implement an abstract parent class?
 */
//////////////////// NOT USED //////////////////////////////////////////////////
public class DeduperFactory {

    private static final Logger LOGGER = 
            LogManager.getLogger(DeduperFactory.class);
    
    private static final Map<BfType, Class<?>> RESOURCE_DEDUPERS =
            new HashMap<BfType, Class<?>>();
    static {
        RESOURCE_DEDUPERS.put(BfType.BF_EVENT, BfResourceDeduper.class);
        RESOURCE_DEDUPERS.put(BfType.BF_FAMILY, BfAuthorityDeduper.class);
        RESOURCE_DEDUPERS.put(BfType.BF_INSTANCE, BfInstanceDeduper.class);
        RESOURCE_DEDUPERS.put(BfType.BF_JURISDICTION,  BfAuthorityDeduper.class);
        RESOURCE_DEDUPERS.put(BfType.BF_MEETING,  BfAuthorityDeduper.class);
        RESOURCE_DEDUPERS.put(BfType.BF_ORGANIZATION,  BfAuthorityDeduper.class);        
        RESOURCE_DEDUPERS.put(BfType.BF_PERSON,  BfAuthorityDeduper.class);
        RESOURCE_DEDUPERS.put(BfType.BF_PLACE,  BfAuthorityDeduper.class);
        RESOURCE_DEDUPERS.put(BfType.BF_TOPIC,  BfTopicDeduper.class);
        RESOURCE_DEDUPERS.put(BfType.BF_WORK,  BfWorkDeduper.class);            
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
        BfType type = BfType.typeForFilename(basename);
        if (type == null) {
            LOGGER.debug("Can't create deduper: no type defined for file "
                   + basename);
            return null;
        }

        // Could use if-thens instead of reflection
        Class<?> deduperClass = RESOURCE_DEDUPERS.get(type);
        if (deduperClass == null) {
            LOGGER.debug("No deduper class defined for type " + type);
            return null;
        }
 
        try {
            Constructor<?> c = deduperClass.getConstructor(BfType.class);
            LOGGER.debug("Constructor is: " + c.getName());
            BfResourceDeduper deduper = (BfResourceDeduper) c.newInstance(type);
            return deduper;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
   
    // If we want to turn the iteration around in ResourceDeduper and iterate
    // over types rather than files, implement this.
    public static BfResourceDeduper createBfResourceDeduper(
            BfType type, String inputDir) {
        // Get file basename from type (return null if null)
        // Get deduper class from TYPE_DEDUPERS (return null if null)
        // Read input file into model
        // Create the deduper, passing in the model
        return null;
    }
            
}
