package org.ld4l.bib2lod;

import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class ProcessorFactory {

    private static final Logger LOGGER = 
            LogManager.getLogger(ProcessorFactory.class);

//    public static <T> T createProcessor(File inputFile, 
//            Map<Ld4lType, Class<?>> typeToProcessorClass) {
//
//        // This case shouldn't occur, since the file comes from an iteration on 
//        // files in the input directory.
//        if (inputFile == null) {
//            LOGGER.debug("Can't create processor: input file is null.");
//            return null;
//        }
//        
//        LOGGER.debug("Creating processor for input file " + inputFile.getName());
//        
//        String basename = FilenameUtils.getBaseName(inputFile.toString());
//        Ld4lType type = Ld4lType.typeForFilename(basename);
//        if (type == null) {
//            LOGGER.debug("Can't create processor: no type defined for file "
//                   + basename);
//            return null;
//        }
//
//        // Could use if-thens instead of reflection
//        Class<?> processorClass = typeToProcessorClass.get(type);
//        if (processorClass == null) {
//            LOGGER.debug("No processor defined for type " + type);
//            return null;
//        }
// 
//        try {
//            return (T) processorClass.getConstructor(
//                    Ld4lType.class).newInstance(type);
//        } catch (Exception e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//            return null;
//        }
//    }
   
    // If we want to turn the iteration around in ResourceDeduper and iterate
    // over types rather than files, implement this.
    public static <T> T createProcessor(
            Ld4lType type, String inputDir, Map<Ld4lType, Class<?>> typeToProcessorClass) {
        // Get file basename from type (return null if null)
        // Get deduper class from TYPE_DEDUPERS (return null if null)
        // Read input file into model
        // Create the deduper, passing in the model
        return null;
    }
            
}
