package org.ld4l.bib2lod.processor.rdfconversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.OntologyType;
import org.ld4l.bib2lod.processor.Processor;
import org.ld4l.bib2lod.processor.rdfconversion.typededuping.TypeDeduper;

// May need to be abstract - we only instantiate PersonDeduper, WorkDeduper, etc.
public class UriDeduper extends Processor {

    private static final Logger LOGGER = LogManager.getLogger(UriDeduper.class);

    /* Define types eligible for deduping.  Two kinds of types are excluded:
     * those where instances are not reused (e.g., Annotation, Title, HeldItem),
     * and those which are not likely to have independent significance (e.g.,
     * Provider is just a carrier for publication data - and more aptly named 
     * Provision). Some of these types are included in the triples for their
     * related type so that they can be used as a basis for deduping: e.g., 
     * Identifiers for Instances, Titles for Works and Instances). 
     */
    private static final List<OntologyType> TYPES_TO_DEDUPE = Arrays.asList(
            OntologyType.BF_PERSON,
            OntologyType.BF_FAMILY,
            OntologyType.BF_JURISDICTION,
            OntologyType.BF_MEETING,
            OntologyType.BF_ORGANIZATION,
            OntologyType.BF_WORK,  
            OntologyType.BF_INSTANCE,
            OntologyType.BF_PLACE,
            OntologyType.BF_TOPIC    
    );
    private static final String REMAINDER = "other";
    // private static final RdfFormat RDF_OUTPUT_FORMAT = RdfFormat.NTRIPLES;
    private Map<String, String> uniqueUris;
    
    public UriDeduper(OntModel bfOntModelInf, String localNamespace, 
            String inputDir, String mainOutputDir) {
                 
        super(bfOntModelInf, localNamespace, inputDir, mainOutputDir);

    }
    
    protected static List<OntologyType> getTypesToDedupe() {
        return TYPES_TO_DEDUPE; 
    }
    
    protected static String getRemainder() {
        return REMAINDER;
    }

//    @Override
//    protected RdfFormat getRdfOutputFormat() {
//        return RDF_OUTPUT_FORMAT;
//    }
    
    @Override
    public String process() {
        LOGGER.trace("Start process");
        
        String outputDir = createOutputDir();
        
        uniqueUris = new HashMap<String, String>();
        File[] inputFiles = new File(inputDir).listFiles();
        
        for ( File file : inputFiles ) {
            Map<String, String> dedupedUris = processInputFile(file);
            if (dedupedUris != null) {
                uniqueUris.putAll(dedupedUris);
            }
        } 
        
        for ( File file : new File(inputDir).listFiles() ) {
            // *** NO We need to also remove duplicates
            // Easiest way to do that is to read into model and read out
            // Or is there a string fn to do this?
            // If dealing with models, maybe we want to do the replacements on
            // models as well...then when we write out the model, duplicates 
            // will be eliminated.
            // FIX THIS!!! 
            replaceLinesInFile(file, outputDir);
        }

        LOGGER.trace("End process");
        return outputDir;
    }

    private Map<String, String> processInputFile(File inputFile) {
        
        String filename = inputFile.getName();
        LOGGER.trace("Start processing file " + filename);
        
        String basename = FilenameUtils.getBaseName(inputFile.toString());
        OntologyType type = OntologyType.getByFilename(basename);
        if (type == null) {
            LOGGER.debug("No type found for file " + basename);
            return null;
        }
        LOGGER.debug("Type = " + type.toString());
        
        try {
            // TODO Consider using a factory instead of defining the deduper
            // in the type. 
            Class<?> cls = type.deduper();
            if (cls == null) {
                return null;
            }
            Model model = readModelFromFile(inputFile);
            // TODO Maybe pass the model in the constructor so it's an instance 
            // variable?
            // TODO Possibly we don't need different deduper subclasses, but
            // just need to define the query for each type in a hash. Then 
            // eliminate TypeDeduper and its subclasses and just use generic
            // methods to execute the query and manage the results.
            TypeDeduper deduper = (TypeDeduper) cls.newInstance();
            return deduper.dedupe(model);
            
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        LOGGER.trace("Done processing file " + filename);
        return null;
    }

    @Override
    protected String processLine(String line) {
        int size = uniqueUris.size();
        String[] originalUris = uniqueUris.keySet().toArray(new String[size]);
        String[] replacementUris = 
                uniqueUris.values().toArray(new String[size]);
        return StringUtils.replaceEach(line, originalUris, replacementUris);
    }

}