package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfResourceDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.DeduperFactory;

/**
 * Converts Bibframe RDF to LD4L RDF.
 * @author rjy7
 *
 */
public class BibframeConverter extends RdfProcessor {

    private static final Logger LOGGER = 
            LogManager.getLogger(BibframeConverter.class);
    
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

        // TEMPORARY
        copyFiles(inputDir, outputDir);
        
        File[] inputFiles = new File(inputDir).listFiles();
        
//        for ( File file : inputFiles ) {
//            String filename = file.getName();
//            LOGGER.debug("Deduping file " + filename);
//            BfResourceDeduper deduper = 
//                    DeduperFactory.createBfResourceDeduper(file);
//            if (deduper == null) {
//                LOGGER.debug("No deduper found for file " + filename);
//                continue;
//            }
//            Model model = readModelFromFile(file);
//           
//        }
        
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


        LOGGER.info("End process");
        return outputDir;        

    }
    
    private void processInputFile(File inputFile) {
        
        String filename = inputFile.getName();
        LOGGER.info("Start processing file " + filename);
        
        String basename = FilenameUtils.getBaseName(inputFile.toString());
        OntType type = OntType.getByFilename(basename);
        if (type == null) {
            LOGGER.warn("No type found for file " + basename);
            return;
        }
        LOGGER.debug("Type = " + type.toString());
    }

}
