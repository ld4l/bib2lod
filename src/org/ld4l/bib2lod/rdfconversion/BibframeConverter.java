package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfAuthorityConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfHeldItemConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfInstanceConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfLanguageConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfMeetingConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfPersonConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfResourceConverter;
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
      
    private static final Map<BfType, Class<?>> CONVERTERS_BY_TYPE =
            new HashMap<BfType, Class<?>>();
    // Commented out converters are called from another converter, so don't
    // need to be included here. If they get called independently, need to be
    // included in this mapping.
    static {
        CONVERTERS_BY_TYPE.put(BfType.BF_AGENT, BfAuthorityConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_ANNOTATION, 
//                BfAnnnotation.class);  
//        CONVERTERS_BY_TYPE.put(BfType.BF_CATEGORY, 
//                BfCategoryConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_CLASSIFICATION, 
//                BfClassificationConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_EVENT, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_FAMILY, BfAuthorityConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_HELD_ITEM, 
                BfHeldItemConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_IDENTIFIER, 
//                BfIdentifierConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_INSTANCE, BfInstanceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_JURISDICTION,  
                BfAuthorityConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_LANGUAGE, BfLanguageConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_MEETING, BfMeetingConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_ORGANIZATION, 
                BfAuthorityConverter.class);        
        CONVERTERS_BY_TYPE.put(BfType.BF_PERSON, BfPersonConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_PLACE, BfAuthorityConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.BF_PROVIDER, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_RESOURCE, BfResourceConverter.class);
        // Temporal is an Authority subtype, but we don't want the bf:label =>
        // foaf:name conversion for Temporal entities.
        CONVERTERS_BY_TYPE.put(BfType.BF_TEMPORAL, BfResourceConverter.class);
//      CONVERTERS_BY_TYPE.put(BfType.BF_TITLE, BfResourceConverter.class);       
        // Maybe can just use BfAuthorityConverter for Topics?
        CONVERTERS_BY_TYPE.put(BfType.BF_TOPIC, BfTopicConverter.class);
        CONVERTERS_BY_TYPE.put(BfType.BF_WORK,  BfWorkConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.MADSRDF_AUTHORITY, 
//                BfResourceConverter.class);
//        CONVERTERS_BY_TYPE.put(BfType.MADSRDF_COMPLEX_SUBJECT,  
//                BfResourceConverter.class); 
    }
    


    public BibframeConverter(OntModel bfOntModelInf,        
            String localNamespace, String inputDir, String mainOutputDir) {
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);
    }

    public BibframeConverter(String localNamespace, String inputDir,
            String mainOutputDir) {
        super(localNamespace, inputDir, mainOutputDir);
    }

    
    @Override
    public String process() {

        Instant processStart = Instant.now();
        LOGGER.info("START Bibframe RDF conversion.");
        
        String outputDir = getOutputDir();  

        int totalSubjectCount = convertFiles(inputDir, outputDir);

        LOGGER.info("END Bibframe RDF conversion in all input files. Converted " 
                + totalSubjectCount + " "     
                + Bib2LodStringUtils.simplePlural("resource", totalSubjectCount)
                + " . " + TimerUtils.getDuration(processStart));

        return outputDir;        
    }
    
    private int convertFiles(String inputDir, String outputDir) {
        
        File[] inputFiles = new File(inputDir).listFiles();
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
        String basename = FilenameUtils.getBaseName(filename);
        
        // LOGGER.debug("FILENAME: " + filename);
        
        if (basename.equals(ResourceDeduper.getNewAssertionsFilename())) {
            // LOGGER.debug("Copying new assertions file.");
            copyFile(file);
            return 0;
        }

        BfType typeForFile;
        
        if (basename.equals(ResourceDeduper.getRemainderFilename())) {
//            // LOGGER.debug("Copying remainder file.");
//            copyFile(file);
//            return 0;
            // This file needs further analysis - at least some statements 
            // should be included in other files
            typeForFile = BfType.BF_RESOURCE;            
        } else {
            typeForFile = BfType.typeForFilename(filename);
        }
        
        // Should not happen
        if (typeForFile == null) {
            return 0;
        }

        String outputFile = FilenameUtils.getBaseName(file.toString());
        
        BfResourceConverter converter = getConverter(typeForFile);
              
        Model inputModel = readModelFromFile(file);
        long inputTripleCount = inputModel.size();

        LOGGER.info("Start converting Bibframe RDF in file " + filename 
                + " containing " + inputTripleCount + " "
                + Bib2LodStringUtils.simplePlural(
                        "triple", inputTripleCount) + ".");

        Instant fileStart = Instant.now();
        
        Model outputModel = ModelFactory.createDefaultModel();
        
        // Iterate over the subjects of the input model
        ResIterator subjects = inputModel.listSubjects();
        
        Instant subjectStart = Instant.now();
        int subjectCount = 0;
        int subjectCountForFile = 0;
        
        while (subjects.hasNext()) {

            Resource inputSubject = subjects.nextResource();
            
            // LOGGER.debug("Found subject " + inputSubject.getURI());

            // Statements about other, "secondary" subjects of other types that 
            // have been  included in this file have distinct converters that 
            // are called from the primary subject's converter. They are 
            // converted with the primary subject because some of these  
            // assertions are needed for or incorporated into conversion of the 
            // primary subject. Examples: Work Titles, Instance Titles and
            // Identifiers.
            if (inputSubject.hasProperty(RDF.type, typeForFile.ontClass()) ||
                    // For file other.nt - convert any subject
                    typeForFile.equals(BfType.BF_RESOURCE)) {
                
                subjectCount++;
                subjectCountForFile++;
                
                // LOGGER.debug("Converting " + typeForFile + " subject " 
                //         + inputSubject.getURI());
                
                // Get the statements about this subject and related objects
                // that must be converted together.
                Resource subject = converter.getSubjectModelToConvert(
                        inputSubject);
	
				outputModel.add(converter.convert(subject));
				
                if (subjectCount == TimerUtils.NUM_ITEMS_TO_TIME) {
                    // TODO Define TIMER logging level between info and debug
                    LOGGER.trace("Converted Bibframe RDF for " + subjectCount 
                            + " " + Bib2LodStringUtils.simplePlural(
                                    "resource", subjectCount)
                            + " in file " + filename + ". " 
                            + TimerUtils.getDuration(subjectStart));
                    subjectCount = 0;
                    subjectStart = Instant.now();
                }   
                
            } else {
                
                // LOGGER.debug("Skipping subject " + inputSubject.getURI()
                //        + " since it's not of type " + typeForFile);                        
            }
        }
        
        if (subjectCount > 0) {
            // TODO Define TIMER logging level between info and debug
            LOGGER.trace("Converted Bibframe RDF for " + subjectCount + " " 
                    + Bib2LodStringUtils.simplePlural("resource", subjectCount)
                    + " in file " + filename + ". " 
                    + TimerUtils.getDuration(subjectStart));  
        }
       
        writeModelToFile(outputModel, outputFile);   
        long outputTripleCount = outputModel.size();
        
        LOGGER.info("Done converting Bibframe RDF in file " + filename 
                + ". Processed " + subjectCountForFile + " " 
                + Bib2LodStringUtils.simplePlural(
                        "resource", subjectCountForFile)
                + " of type " + typeForFile.uri() + " and generated " 
                + outputTripleCount + " " + 
                Bib2LodStringUtils.simplePlural("triple", outputTripleCount) 
                + ". " + TimerUtils.getDuration(fileStart));
        
        return subjectCountForFile;
    }

     
    private BfResourceConverter getConverter(BfType bfType) {
        
        Class<?> converterClass = CONVERTERS_BY_TYPE.get(bfType);
        if (converterClass != null) {
            try {
                BfResourceConverter converter = 
                        (BfResourceConverter) converterClass
                        .getConstructor(BfType.class, String.class)
                        .newInstance(bfType, localNamespace);   
                // LOGGER.debug("Got converter for type " + bfType);
                return converter;
            } catch (Exception e) {
                LOGGER.warn("Can't instantiate class " 
                        + converterClass.getName());
                e.printStackTrace();
            } 
        }

        // LOGGER.debug("No converter found for type " + bfType);
        return null;
    }

}
