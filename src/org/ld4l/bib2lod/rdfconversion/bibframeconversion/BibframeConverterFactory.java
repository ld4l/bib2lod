package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntType;


/* TODO Much of this code is repeated in converterFactory. Combine if possible.
 * Maybe implement an abstract parent class?
 */
public class BibframeConverterFactory {

    private static final Logger LOGGER = 
            LogManager.getLogger(BibframeConverterFactory.class);
    

    private static final Map<OntType, Class<?>> BF_CONVERTERS =
            new HashMap<OntType, Class<?>>();
    static {
        // Assign temporarily to BfResourceConverter till start implementing
        // type-specific class
        BF_CONVERTERS.put(OntType.BF_EVENT, BfResourceConverter.class);
        BF_CONVERTERS.put(OntType.BF_FAMILY, BfResourceConverter.class);
        BF_CONVERTERS.put(OntType.BF_INSTANCE, BfResourceConverter.class);
        BF_CONVERTERS.put(OntType.BF_JURISDICTION,  BfResourceConverter.class);
        BF_CONVERTERS.put(OntType.BF_MEETING,  BfResourceConverter.class);
        BF_CONVERTERS.put(OntType.BF_ORGANIZATION,  BfResourceConverter.class);        
        BF_CONVERTERS.put(OntType.BF_PERSON,  BfPersonConverter.class);
        BF_CONVERTERS.put(OntType.BF_PLACE,  BfResourceConverter.class);
        BF_CONVERTERS.put(OntType.BF_TOPIC,  BfResourceConverter.class);
        BF_CONVERTERS.put(OntType.BF_WORK,  BfResourceConverter.class);            
    }
    
    public static BfResourceConverter createBfResourceConverter(File inputFile) {

        // This case shouldn't occur since the file comes from an iteration on 
        // files in the input directory.
        if (inputFile == null) {
            LOGGER.info("Can't create converter: input file is null.");
            return null;
        }
        
        LOGGER.info("Creating converter for input file " + inputFile.getName());
        
        String basename = FilenameUtils.getBaseName(inputFile.toString());
        OntType type = OntType.typeForFilename(basename);
        if (type == null) {
            LOGGER.info("Can't create converter: no type defined for file "
                   + basename);
            return null;
        }

        // Could use if-thens instead of reflection
        Class<?> converterClass = BF_CONVERTERS.get(type);
        if (converterClass == null) {
            LOGGER.info("No converter class defined for type " + type);
            return null;
        }
 
        try {
            Constructor<?> c = converterClass.getConstructor(OntType.class);
            BfResourceConverter converter = (BfResourceConverter) c.newInstance(type);
            return converter;
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        }
    }
   
    // If we want to turn the iteration around in ResourceConverter and iterate
    // over types rather than files, implement this.
    public static BfResourceConverter createBfResourceConverter(
            OntType type, String inputDir) {
        // Get file basename from type (return null if null)
        // Get converter class from TYPE_converterS (return null if null)
        // Read input file into model
        // Create the converter, passing in the model
        return null;
    }
            
}