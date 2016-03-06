package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
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
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfMeetingConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfPersonConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfProviderConverter;
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
    private static Map<BfType, BfResourceConverter> CONVERTERS =
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
        // However, if there are later features that should be derived from
        // BfAuthorityConverter, create a subtype of BfAuthorityConverter, 
        // and add a mapping BfProperty.BF_LABEL => Ld4lProperty.LABEL.
        CONVERTERS_BY_TYPE.put(BfType.BF_TEMPORAL, BfResourceConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_EVENT, BfResourceConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_LANGUAGE, BfLanguageConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_IDENTIFIER, 
                BfIdentifierConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.BF_PROVIDER, BfProviderConverter.class);
        
        // TODO Needs own converter
        CONVERTERS_BY_TYPE.put(BfType.BF_CATEGORY, BfResourceConverter.class);
                
        
        CONVERTERS_BY_TYPE.put(BfType.BF_CLASSIFICATION, 
                BfResourceConverter.class);

        CONVERTERS_BY_TYPE.put(BfType.BF_ANNOTATION, 
                BfAnnotationConverter.class);  
        CONVERTERS_BY_TYPE.put(BfType.BF_REVIEW, BfAnnotationConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_SUMMARY, BfAnnotationConverter.class);
        
        CONVERTERS_BY_TYPE.put(BfType.MADSRDF_AUTHORITY, 
                BfResourceConverter.class);       
        
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
                    // LOGGER.debug("Created converter for type " + bfType);
    
                } catch (Exception e) {
                    LOGGER.warn("Can't instantiate class " 
                            + converterClass.getName());
                    e.printStackTrace();
                    return;
                }   
                
                instantiatedClasses.put(converterClass, converter);
                
            } else {
                // LOGGER.debug("Converter for class " + converterClass 
                //         + " already created.");
                converter = instantiatedClasses.get(converterClass);
            }
            
            CONVERTERS.put(bfType, converter);
        }
    }

    @Override
    public String process() {

        Instant processStart = Instant.now();
        LOGGER.info("START Bibframe RDF conversion.");
        
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
            convertFile(file, fileCount, totalFileCount);          
        }  

        LOGGER.info("END Bibframe RDF conversion of all input files. "
                + "Duration: " + TimerUtils.getDuration(processStart) + ".");

        return outputDir;        
    }
    
    private void convertFile(File file, int fileCount, int totalFileCount) {
        
        String filename = file.getName();

        LOGGER.info("Start Bibframe RDF conversion of file "                
                + filename + " (file " + fileCount + " of " 
                + Bib2LodStringUtils.count(totalFileCount, "input file")
                + ").");

        Instant fileStart = Instant.now();
        
        Model inputModel = readModelFromFile(file);

        Model outputModel = ModelFactory.createDefaultModel();

        List<Resource> resourcesToRemove = new ArrayList<Resource>();
        
        // Iterate through the types in the specified order
        int subjectCount = 0;
        for (Map.Entry<BfType, BfResourceConverter> entry: 
                CONVERTERS.entrySet()) {
 
            BfType bfType = entry.getKey();
            BfResourceConverter converter = entry.getValue();
            
            subjectCount += convertResourceType(bfType, converter, inputModel, 
                    outputModel, resourcesToRemove);
        }
   
        String basename = FilenameUtils.getBaseName(file.toString());
        writeModelToFile(outputModel, basename); 
        
        LOGGER.info("End Bibframe RDF conversion of file " + filename 
                + " (file " + fileCount + " of " 
                + Bib2LodStringUtils.count(totalFileCount, "input file") + "). "
                + "Converted "
                + Bib2LodStringUtils.count(inputModel.size(), "triple") + " "
                + "with "
                + Bib2LodStringUtils.count(subjectCount, "subject") + " to "                 
                + Bib2LodStringUtils.count(outputModel.size(), "triple") + ". "                     
                + "Duration: " + TimerUtils.getDuration(fileStart) + ".");               
                
        inputModel.close();
        outputModel.close();
    }
    
    private int convertResourceType(
            BfType bfType, BfResourceConverter converter, Model inputModel, 
            Model outputModel, List<Resource> resourcesToRemove) {
    
        int subjectCount = 0;
        
        // Get all the subjects of this type
        ResIterator subjects = inputModel.listResourcesWithProperty(
                RDF.type, bfType.ontClass());

        // Iterate through the subjects of this type and convert
        while (subjects.hasNext()) {
            subjectCount++;
            Resource subject = subjects.nextResource();
            convertSubject(subject, converter, outputModel, resourcesToRemove);
        }                   

        return subjectCount;
    }
    
    private void convertSubject(Resource subject, BfResourceConverter converter,
            Model outputModel, List<Resource> resourcesToRemove) {

        if (resourcesToRemove.contains(subject)) {
            // If a previous converter has designated this resource for
            // removal, do not process it, and do not add it to the 
            // output model. Example: a Meeting removes the associated
            // madsrdf:Authority, since in LD4L a Meeting is an Event
            // rather than an Authority.
            LOGGER.debug("Removing subject " + subject.getURI());
            resourcesToRemove.remove(subject);  
            
        } else {
        
            LOGGER.debug("Processing subject " + subject.getURI());
            
            // Convert the subject and add to the output model for the file.
            Model convertedModel = converter.convert(subject);
            outputModel.add(convertedModel);
            convertedModel.close();  
            
            // Get the resources the converter has designated for 
            // removal. These will be tested on subsequent iterations
            // through the subjects iterator, and if it is in the list,
            // conversion will be skipped over.
            resourcesToRemove.addAll(converter.getResourcesToRemove());
        }
    }
}
