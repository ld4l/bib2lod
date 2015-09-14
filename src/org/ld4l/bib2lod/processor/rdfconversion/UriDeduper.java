package org.ld4l.bib2lod.processor.rdfconversion;

import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.OntologyType;
import org.ld4l.bib2lod.RdfFormat;
import org.ld4l.bib2lod.processor.Processor;

// May need to be abstract - we only instantiate PersonDeduper, WorkDeduper, etc.
public class UriDeduper extends Processor {

    @SuppressWarnings("unused")
    private static final Logger LOGGER = LogManager.getLogger(UriDeduper.class);

    /* Define types eligible for deduping.  Two kinds of types are excluded:
     * those where instances are not reused (e.g., Annotation, Title, HeldItem),
     * and those which are not likely to have independent significance (e.g.,
     * Provider is just a carrier for publication data - and more aptly named 
     * Provision). Some of these types are included in the triples for their
     * related type so that they can be used as a basis for deduping: e.g., 
     * Identifiers for Instances, Titles for Works and Instances).
     */
    private static final LinkedHashSet<OntologyType> TYPES_TO_DEDUPE = 
            new LinkedHashSet<OntologyType>(Arrays.asList(
                OntologyType.BF_PERSON,
                OntologyType.BF_FAMILY,
                OntologyType.BF_JURISDICTION,
                OntologyType.BF_MEETING,
                OntologyType.BF_ORGANIZATION,
                OntologyType.BF_WORK,  
                OntologyType.BF_INSTANCE,
                OntologyType.BF_PLACE,
                OntologyType.BF_TOPIC)    
    );

    private static final RdfFormat RDF_OUTPUT_FORMAT = RdfFormat.NTRIPLES;
    
    public UriDeduper(OntModel bfOntModelInf, String localNamespace, 
            String inputDir, String mainOutputDir) {
                 
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);
        
        for (OntologyType s : TYPES_TO_DEDUPE) {
            LOGGER.debug(s.uri());
        }
    }
    
    protected static LinkedHashSet<OntologyType> getTypesToDedupe() {
        return TYPES_TO_DEDUPE;

    }

    @Override
    protected RdfFormat getRdfOutputFormat() {
        return RDF_OUTPUT_FORMAT;
    }
    
    @Override
    public String process() {
        LOGGER.trace("Start process");
        
        String outputDir = createOutputDir();
        
        for (OntologyType type : TYPES_TO_DEDUPE) {
            String filename = type.filename();
            LOGGER.debug(filename);
        }
        
        for ( File file : new File(inputDir).listFiles() ) {
            String filename = file.getName();
            LOGGER.trace("Start processing file " + filename);
            Model outputModel = processInputFile(file);
            String outputFilename = getOutputFilename(
                    FilenameUtils.getBaseName(file.toString()));
            File outputFile = new File(outputDir, outputFilename); 
            writeModelToFile(outputModel, outputFile);
            LOGGER.trace("Done processing file " + filename);
        }   
        
        LOGGER.trace("End process");
        return outputDir;
    }
    
    private Model processInputFile(File inputFile) {
        Model inputModel = readModelFromFile(inputFile);
        
        return inputModel;
    }

}