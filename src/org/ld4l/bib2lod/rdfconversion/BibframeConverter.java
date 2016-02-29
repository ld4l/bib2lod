package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfAnnotationConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfAuthorityConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfHeldItemConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfIdentifierConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfInstanceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfLanguageConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.MadsAuthorityConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfMeetingConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfPersonConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfResourceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfTitleConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfTopicConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfWorkConverter;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;

/**
 * Converts Bibframe RDF to LD4L RDF.
 * @author rjy7
 *
 */
public class BibframeConverter extends RdfProcessor {

    private static final Logger LOGGER = 
            LogManager.getLogger(BibframeConverter.class);

    // Order may matter, so use LinkedHashMap
    private static Map<BfType, BfResourceConverter> converters =
            new LinkedHashMap<BfType, BfResourceConverter>();
    
    private static final Map<BfType, Class<?>> CONVERTERS_BY_TYPE =
            new LinkedHashMap<BfType, Class<?>>();
    static {
        CONVERTERS_BY_TYPE.put(BfType.BF_WORK,  BfWorkConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_INSTANCE, BfInstanceConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_HELD_ITEM, 
                BfHeldItemConverter.class);

        CONVERTERS_BY_TYPE.put(BfType.BF_TOPIC, BfTopicConverter.class);

        CONVERTERS_BY_TYPE.put(BfType.BF_TITLE, BfTitleConverter.class);   

        CONVERTERS_BY_TYPE.put(BfType.BF_PERSON, BfPersonConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_ORGANIZATION, 
                BfAuthorityConverter.class);   
        CONVERTERS_BY_TYPE.put(BfType.BF_FAMILY, BfAuthorityConverter.class); 
        CONVERTERS_BY_TYPE.put(BfType.BF_JURISDICTION,  
                BfAuthorityConverter.class);   
        CONVERTERS_BY_TYPE.put(BfType.BF_AGENT, BfAuthorityConverter.class);

        CONVERTERS_BY_TYPE.put(BfType.BF_PLACE, BfAuthorityConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_MEETING, BfMeetingConverter.class);
        
        // Temporal is an Authority subtype, but we don't want the bf:label =>
        // foaf:name conversion for Temporal entities.
        CONVERTERS_BY_TYPE.put(BfType.BF_TEMPORAL, BfResourceConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_EVENT, BfResourceConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_LANGUAGE, BfLanguageConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_IDENTIFIER, 
                BfIdentifierConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_PROVIDER, BfResourceConverter.class);
        
        // TODO Needs own converter
        CONVERTERS_BY_TYPE.put(BfType.BF_CATEGORY, 
                BfResourceConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_CLASSIFICATION, 
                BfResourceConverter.class);

        CONVERTERS_BY_TYPE.put(BfType.BF_ANNOTATION, 
                BfAnnotationConverter.class);  

        // TODO Needs own converter
        CONVERTERS_BY_TYPE.put(BfType.MADSRDF_AUTHORITY, 
                MadsAuthorityConverter.class);       
        
        CONVERTERS_BY_TYPE.put(BfType.BF_RESOURCE, BfResourceConverter.class);
    }
    

    public BibframeConverter(String localNamespace, String inputDir,
            String mainOutputDir) {
        super(localNamespace, inputDir, mainOutputDir);
        
        createConverters();
    }
   
    private void createConverters() {

        Map<Class<?>, BfResourceConverter> instantiatedClasses = 
                new HashMap<Class<?>, BfResourceConverter>();
        
        for (Map.Entry<BfType, Class<?>> entry : 
                CONVERTERS_BY_TYPE.entrySet()) {
            
            BfType bfType = entry.getKey();
            Class<?> converterClass = entry.getValue();
            
            BfResourceConverter converter;
            
            if (! instantiatedClasses.keySet().contains(converterClass)) {
                
                try {
                    converter = (BfResourceConverter) converterClass                            
                            .getConstructor(String.class)
                            .newInstance(localNamespace);   
                    LOGGER.debug("Created converter for type " + bfType);
    
                } catch (Exception e) {
                    LOGGER.warn("Can't instantiate class " 
                            + converterClass.getName());
                    e.printStackTrace();
                    return;
                }   
                
                instantiatedClasses.put(converterClass, converter);
                
            } else {
                LOGGER.debug("Converter for class " + converterClass 
                        + " already created.");
                converter = instantiatedClasses.get(converterClass);
            }
            
            converters.put(bfType, converter);
        }
    }

    @Override
    public String process() {

        Instant processStart = Instant.now();
        LOGGER.info("START Bibframe RDF conversion.");
        
        String outputDir = getOutputDir();  

        int totalSubjectCount = convertFiles(inputDir, outputDir);

        LOGGER.info("END Bibframe RDF conversion in all input files. Converted " 
                + totalSubjectCount + " "     
                + Bib2LodStringUtils.simplePlural(totalSubjectCount, "resource")
                + " . " + TimerUtils.getDuration(processStart));

        return outputDir;        
    }
    
    private int convertFiles(String inputDir, String outputDir) {
        
        File[] inputFiles = new File(inputDir).listFiles();
        
        // For consistent ordering. Helps locate errors if program exits
        // unexpectedly. Time to sort is miniscule (0.008 seconds on 34,540 
        // files).
        Arrays.sort(inputFiles);

        int totalSubjectCount = 0;
        
        for ( File file : inputFiles ) {
            totalSubjectCount += convertFile(file);
        }  

        /*
         * Possibly we need other steps here: go back through files to do URI
         * replacements. This entails keeping separate models for statements 
         * that shouldn't undergo URI replacement: e.g., the new statements
         * we create linking ld4l entities to bf entities, where the latter
         * shouldn't undergo URI replacement.
         */
        
        return totalSubjectCount;
    }

    private int convertFile(File file) {
        
        String filename = file.getName();

        Model inputModel = readModelFromFile(file);
        
        int subjectCount = 0;

        LOGGER.info("Start converting Bibframe RDF in file " + filename 
                + " containing " + Bib2LodStringUtils.count(
                        inputModel.size(), "triple") + ".");

        Instant fileStart = Instant.now();

        Model outputModel = ModelFactory.createDefaultModel();

        // Iterate through the types in the specified order
        for (Map.Entry<BfType, BfResourceConverter> entry: 
                converters.entrySet()) {
            
            BfType bfType = entry.getKey();
            BfResourceConverter converter = entry.getValue();
            
            // Get all the subjects of this type
            ResIterator subjects = inputModel.listResourcesWithProperty(
                    RDF.type, bfType.ontClass());
            
            // Iterate through the subjects of this type and convert
            while (subjects.hasNext()) {
                subjectCount++;
                Resource subject = subjects.nextResource();
                
                // Convert the subject and add to the output model for the file.
                Model convertedModel = converter.convert(subject);
                outputModel.add(convertedModel);
                
                convertedModel.close();                
            }           
        }

        inputModel.close();
        
        String outputFile = FilenameUtils.getBaseName(file.toString());
        writeModelToFile(outputModel, outputFile); 
        
        LOGGER.info("Converted " 
                + Bib2LodStringUtils.count(subjectCount, "subject") 
                + " in file "
                + filename + " resulting in " + Bib2LodStringUtils.count(
                        outputModel.size(), "triple") + "." 
                + TimerUtils.getDuration(fileStart));

        
        outputModel.close();
        
        return subjectCount;
    }

}
