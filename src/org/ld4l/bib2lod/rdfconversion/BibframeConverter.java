package org.ld4l.bib2lod.rdfconversion;

import java.io.File;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
        // TODO Auto-generated constructor stub
    }

    @Override
    public String process() {
        
        String outputDir = getOutputDir();  
        
        File[] inputFiles = new File(inputDir).listFiles();
        
        for ( File file : inputFiles ) {
            processInputFile(file);
        }
        
        return outputDir;

    }
    
    private void processInputFile(File inputFile) {
        
        String filename = inputFile.getName();
        LOGGER.trace("Start processing file " + filename);
        
        String basename = FilenameUtils.getBaseName(inputFile.toString());
        OntType type = OntType.getByFilename(basename);
        if (type == null) {
            LOGGER.warn("No type found for file " + basename);
            return;
        }
        LOGGER.debug("Type = " + type.toString());
    }

}
