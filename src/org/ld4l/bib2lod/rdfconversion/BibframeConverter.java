package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;         
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfPersonConverter;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfResourceConverter;

/**
 * Converts Bibframe RDF to LD4L RDF.
 * @author rjy7
 *
 */
public class BibframeConverter extends RdfProcessor {

    private static final Logger LOGGER = 
            LogManager.getLogger(BibframeConverter.class);
      
    private static final Map<OntType, Class<?>> CONVERTERS_BY_TYPE =
            new HashMap<OntType, Class<?>>();
    static {
        // Assign temporarily to BfResourceConverter till start implementing
        // type-specific converter classes.
        CONVERTERS_BY_TYPE.put(OntType.BF_ANNOTATION, 
                BfResourceConverter.class);            
        CONVERTERS_BY_TYPE.put(OntType.BF_EVENT, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_FAMILY, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_HELD_ITEM, 
                BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_IDENTIFIER, 
                BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_INSTANCE, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_JURISDICTION,  
                BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_MEETING, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_ORGANIZATION, 
                BfResourceConverter.class);        
        CONVERTERS_BY_TYPE.put(OntType.BF_PERSON, BfPersonConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_PLACE, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_PROVIDER, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_TITLE, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_TOPIC, BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.BF_WORK,  BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.MADSRDF_AUTHORITY, 
                BfResourceConverter.class);
        CONVERTERS_BY_TYPE.put(OntType.MADSRDF_COMPLEX_SUBJECT,  
                BfResourceConverter.class); 
    }
    
//    private static final String IN_PROGRESS_DIR = "inProgress";
    private static final String NEW_ASSERTIONS_FILENAME = 
            ResourceDeduper.getNewAssertionsFilename();
    
    
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

        LOGGER.info("Start Bibframe conversion process");
        
        String outputDir = getOutputDir();  

        convertFiles(inputDir, outputDir);

        LOGGER.info("End Bibframe conversion process");
        return outputDir;        
    }
    
    private void convertFiles(String inputDir, String outputDir) {
        
        File[] inputFiles = new File(inputDir).listFiles();
        
        for ( File file : inputFiles ) {
            convertFile(file);
        }  

        /*
         * Possibly we need other steps here: go back through files to do URI
         * replacements. This entails keeping separate models for statements 
         * that shouldn't undergo URI replacement: e.g., the new statements
         * we create linking ld4l entities to bf entities, where the latter
         * shouldn't undergo URI replacement.
         */
    }
    
    /*
     * If as the last step in ResourceDeduper we type-split again, separating 
     * out all statements by subject type (i.e., put Titles into their own file
     * rather than combined with bfWork or bfInstance, etc.), then we could
     * derive the converter type from the filename, as for ResourceDeduper.
     * (However, if there's still an other.nt file we'd have to deal with that
     * as here.) Consider implementing this step.
     * On the other hand, there's an advantage that in the current 
     * implementation it doesn't matter how the input files are structured - we
     * wouldn't even need the initial type-splitting or deduping, so the 
     * processes are more independent. (We could eliminate the prerequisite on
     * Bibframe conversion, though deduping still depends on type-splitting.)
     */
    private BfResourceConverter getConverterForModel(
            Resource subject, Model model) {
        
        // Will need modification if ordering of types is crucial.
        for (Map.Entry<OntType, Class<?>> entry : 
                CONVERTERS_BY_TYPE.entrySet()) {
            OntType type = entry.getKey();
            Resource ontClass = model.createResource(type.uri());
            LOGGER.trace("Checking subject " + subject.getURI() + " and type " 
                        + type.uri());
            if (model.contains(null, RDF.type, ontClass)) {
                LOGGER.trace("Found converter class for subject " 
                        + subject.getURI() + " of type " + type.uri());                        
                return createConverter(type, entry.getValue());
            }
        }
        
        LOGGER.debug("No converter found for subject" + subject.getURI());
        return null;
    }

    private BfResourceConverter createConverter(
            OntType type, Class<?> converterClass) {
        
        try {
            BfResourceConverter converter = 
                    (BfResourceConverter) converterClass
                    .getConstructor(OntType.class).newInstance(type);                        
            return converter;      
        } catch (Exception e) {
            LOGGER.info("No converter created for type " + type);
            // TODO Auto-generated catch block
            e.printStackTrace();
        }    
        
        return null;
    }
    
    private void convertFile(File file) {
        
        String filename = file.getName();
        String outputFile = FilenameUtils.getBaseName(file.toString());
        
        // No need to convert new statements that have been added during
        // processing.
        if (outputFile.equals(NEW_ASSERTIONS_FILENAME)) {
            LOGGER.debug("Copying file " + filename);
            copyFile(file);
            return;
        }

        LOGGER.trace("Processing file " + filename);
        
        Model inputModel = readModelFromFile(file);  
        Model outputModel = ModelFactory.createDefaultModel();
        
        ResIterator subjects = inputModel.listSubjects();
        while (subjects.hasNext()) {

            Resource subject = subjects.nextResource();
            StmtIterator statements = 
                    inputModel.listStatements(subject, null, (RDFNode) null);
            Model subjectModel = ModelFactory.createDefaultModel();
            subjectModel.add(statements);
            BfResourceConverter converter = 
                    getConverterForModel(subject, subjectModel);
            if (converter == null) {
                LOGGER.trace("No converter found for subject " 
                        + subject.getURI());
                outputModel.add(subjectModel);
            } else {
                Model newSubjectModel = converter.convert(subjectModel);
                outputModel.add(newSubjectModel);
            }
        }
                    
        LOGGER.debug("Writing model to file " + filename);
        writeModelToFile(outputModel, outputFile);
        
    }

}
