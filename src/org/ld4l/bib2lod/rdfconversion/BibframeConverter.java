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
    
    private static final String IN_PROGRESS_DIR = "inProgress";
    private static final String NEW_STATEMENT_FILENAME = 
            ResourceDeduper.getNewStatementFilename();
    
    private Map<OntType, BfResourceConverter> converters; 
    
    
    public BibframeConverter(OntModel bfOntModelInf,        
            String localNamespace, String inputDir, String mainOutputDir) {
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);
        converters = createConverters();
    }

    public BibframeConverter(String localNamespace, String inputDir,
            String mainOutputDir) {
        super(localNamespace, inputDir, mainOutputDir);
        converters = createConverters();
    }

    private Map<OntType, BfResourceConverter> createConverters() {
        
        Map<OntType, BfResourceConverter> converters = 
                new HashMap<OntType, BfResourceConverter>();
        
        for (Map.Entry<OntType, Class<?>> entry : 
                CONVERTERS_BY_TYPE.entrySet()) {
            OntType type = entry.getKey();
            Class<?> converterClass = entry.getValue();

            try {
                BfResourceConverter converter = 
                        (BfResourceConverter) converterClass
                        .getConstructor(OntType.class).newInstance(type);                        
                converters.put(type,  converter);      
            } catch (Exception e) {
                LOGGER.info("No converter created for type " + type);
                // TODO Auto-generated catch block
                e.printStackTrace();
            }              
        }
        
        return converters;
    }
    @Override
    public String process() {

        LOGGER.info("Start Bibframe conversion process");
        
        String outputDir = getOutputDir();  

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
//          for ( OntType type : TYPES_TO_DEDUPE ) {
//          BfResourceDeduper deduper = 
//                  DeduperFactory.createBfResourceDeduper(type, inputDir);
//          if (deduper == null) {
//              LOGGER.debug("No deduper found for type " + type);
//              continue;
//          }
//          Map<String, String> dedupedUris = deduper.dedupe();
//          if (dedupedUris != null) {
//              uniqueUris.putAll(dedupedUris);
//          }
//          
//          Model statements = deduper.getNewStatements();
//          if (statements != null) {
//              newStatements.add(statements);
//          }
//  }
        
        File[] inputFiles = new File(inputDir).listFiles();
        
        for ( File file : inputFiles ) {
            
            String outputFile = FilenameUtils.getBaseName(file.toString());
            
            // No need to convert new statements that have been added during
            // processing.
            if (FilenameUtils.getBaseName(
                    file.toString()).equals(NEW_STATEMENT_FILENAME)) {
                LOGGER.debug("Copying file " + NEW_STATEMENT_FILENAME);
                copyFile(file, outputDir);
                continue;
            }
            
            LOGGER.trace("Processing file " + file.getName());
            
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
                        
            LOGGER.debug("Writing model to file " + outputFile);
            writeModelToFile(outputModel, outputFile);
            
        }
        
        LOGGER.info("End Bibframe conversion process");
        return outputDir;        

    }
    
    /*
     * If as the last step in ResourceDeduper we type-split again, separating 
     * out all statements by subject type (i.e., put Titles into their own file
     * rather than combined with bfWork or bfInstance, etc.), then we could
     * derive the converter type from the filename, as for ResourceDeduper.
     * (However, if there's still an other.nt file we'd have to deal with that
     * as here.) Consider implementing this step.
     * Note, however, that in the current implementation it doesn't matter how 
     * the input files are structured - we don't even need the initial type-
     * splitting or deduping. (So could eliminate the prerequisites on Bibframe
     * conversion.)
     */
    private BfResourceConverter getConverterForModel(
            Resource subject, Model model) {
        
        // This will need modification if ordering of types is crucial.
        for (Map.Entry<OntType, BfResourceConverter> entry : 
            converters.entrySet()) {
            OntType type = entry.getKey();
            Resource ontClass = model.createResource(type.uri());
            LOGGER.trace("Checking subject " + subject.getURI() + " and type " 
                        + type.uri());
            if (model.contains(null, RDF.type, ontClass)) {
                LOGGER.trace("Found converter for subject " + subject.getURI() 
                        + " of type " + type.uri());
                return entry.getValue();
            }
        }
        
        return null;
    }
    
//    private void processInputFile(File inputFile) {
//        
//        String filename = inputFile.getName();
//        LOGGER.info("Start processing file " + filename);
//        
//        String basename = FilenameUtils.getBaseName(inputFile.toString());
//        OntType type = OntType.getByFilename(basename);
//        if (type == null) {
//            LOGGER.warn("No type found for file " + basename);
//            return;
//        }
//        LOGGER.debug("Type = " + type.toString());
//    }

}
