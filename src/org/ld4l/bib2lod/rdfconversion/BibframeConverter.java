package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.ProcessorFactory;
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
      
    private static final Map<OntType, Class<?>> BIBFRAME_CONVERTERS =
            new HashMap<OntType, Class<?>>();
    static {
        // Assign temporarily to BfResourceConverter till start implementing
        // type-specific converter classes.
        BIBFRAME_CONVERTERS.put(OntType.BF_EVENT, BfResourceConverter.class);
        BIBFRAME_CONVERTERS.put(OntType.BF_FAMILY, BfResourceConverter.class);
        BIBFRAME_CONVERTERS.put(OntType.BF_HELD_ITEM, 
                BfResourceConverter.class);
        BIBFRAME_CONVERTERS.put(OntType.BF_INSTANCE, BfResourceConverter.class);
        BIBFRAME_CONVERTERS.put(OntType.BF_JURISDICTION,  
                BfResourceConverter.class);
        BIBFRAME_CONVERTERS.put(OntType.BF_MEETING, BfResourceConverter.class);
        BIBFRAME_CONVERTERS.put(OntType.BF_ORGANIZATION, 
                BfResourceConverter.class);        
        BIBFRAME_CONVERTERS.put(OntType.BF_PERSON, BfPersonConverter.class);
        BIBFRAME_CONVERTERS.put(OntType.BF_PLACE, BfResourceConverter.class);
        BIBFRAME_CONVERTERS.put(OntType.BF_TOPIC, BfResourceConverter.class);
        BIBFRAME_CONVERTERS.put(OntType.BF_WORK,  BfResourceConverter.class);            
    }
    
    
    public BibframeConverter(OntModel bfOntModelInf,
            String localNamespace, String inputDir, String mainOutputDir) {
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);
        // TODO Auto-generated constructor stub
    }

    public BibframeConverter(String localNamespace, String inputDir,
            String mainOutputDir) {
        super(localNamespace, inputDir, mainOutputDir);
    }

    @Override
    public String process() {

        LOGGER.info("Start process");
        
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
            String filename = file.getName();
            String basename = FilenameUtils.getBaseName(file.toString());
            LOGGER.debug("Converting file " + filename);
            OntType type = OntType.getByFilename(basename);
            if (type == null) {
                LOGGER.debug("No converter found for file " + filename);
                // TODO *** What to do here? - 
                // we still need to convert other.nt ***
                // Unless we've gotten rid of all other statements
                // Either (a) eliminate other file by putting all statements
                // into a type file, or (b) query the other model to get the
                // types represented in the file. Then for each type, 
                // get a converter and convert. Would be a reason to store
                // the converter instances in an instance variable here, so 
                // they can be re-used, like the dedupers.
                // NB File newStatements.nt should not be converted.                
                continue;
            }
            LOGGER.debug("Type = " + type);
            Class<?> converterClass = BIBFRAME_CONVERTERS.get(type);
            LOGGER.debug("Found class " + converterClass.toString() 
                    + " for type " + type);
            try {
                BfResourceConverter converter = (BfResourceConverter) 
                        converterClass.getConstructor(
                                OntType.class).newInstance(type);
            } catch (Exception e) {
                LOGGER.debug("No converter created for type " + type);
                // TODO Auto-generated catch block
                e.printStackTrace();
            }    

            Model model = readModelFromFile(file);          
        }

        // TEMPORARY
        copyFiles(inputDir, outputDir);
        
        LOGGER.info("End process");
        return outputDir;        

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
