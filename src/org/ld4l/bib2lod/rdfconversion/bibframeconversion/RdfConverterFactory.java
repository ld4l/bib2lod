package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntType;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfAgentDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfInstanceDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfResourceDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfTopicDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfWorkDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.DeduperFactory;

public class RdfConverterFactory {

    private static final Logger LOGGER = 
            LogManager.getLogger(RdfConverterFactory.class);
    
    private static final Map<OntType, Class<?>> RESOURCE_CONVERTERS =
            new HashMap<OntType, Class<?>>();
    static {
        // Assign temporarily to BfResourceConverter till start implementing
        // type-specific class
        RESOURCE_CONVERTERS.put(OntType.BF_EVENT, BfResourceConverter.class);
        RESOURCE_CONVERTERS.put(OntType.BF_FAMILY, BfResourceConverter.class);
        RESOURCE_CONVERTERS.put(OntType.BF_INSTANCE, BfResourceConverter.class);
        RESOURCE_CONVERTERS.put(OntType.BF_JURISDICTION,  BfResourceConverter.class);
        RESOURCE_CONVERTERS.put(OntType.BF_MEETING,  BfResourceConverter.class);
        RESOURCE_CONVERTERS.put(OntType.BF_ORGANIZATION,  BfResourceConverter.class);        
        RESOURCE_CONVERTERS.put(OntType.BF_PERSON,  BfPersonConverter.class);
        RESOURCE_CONVERTERS.put(OntType.BF_PLACE,  BfResourceConverter.class);
        RESOURCE_CONVERTERS.put(OntType.BF_TOPIC,  BfResourceConverter.class);
        RESOURCE_CONVERTERS.put(OntType.BF_WORK,  BfResourceConverter.class);            
    }
    
    public static BfResourceDeduper createBfResourceDeduper(File inputFile) {

        // This case shouldn't occur since the file comes from an iteration on 
        // files in the input directory.
        if (inputFile == null) {
            LOGGER.debug("Can't create converter: input file is null.");
            return null;
        }
        
        LOGGER.debug("Creating converter for input file " + inputFile.getName());
        
        String basename = FilenameUtils.getBaseName(inputFile.toString());
        OntType type = OntType.getByFilename(basename);
        if (type == null) {
            LOGGER.debug("Can't create converter: no type defined for file "
                   + basename);
            return null;
        }

        // Could use if-thens instead of reflection
        Class<?> converterClass = RESOURCE_CONVERTERS.get(type);
        if (converterClass == null) {
            LOGGER.debug("No converter class defined for type " + type);
            return null;
        }
 
        try {
            return (BfResourceDeduper) converterClass.getConstructor(
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
        // Get converter class from TYPE_DEDUPERS (return null if null)
        // Read input file into model
        // Create the converter, passing in the model
        return null;
    }

}
