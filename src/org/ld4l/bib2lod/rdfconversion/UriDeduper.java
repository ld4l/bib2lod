package org.ld4l.bib2lod.rdfconversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.typededuping.TypeDeduper;

// May need to be abstract - we only instantiate PersonDeduper, WorkDeduper, etc.
public class UriDeduper extends RdfProcessor {

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
            //OntologyType.MADSRDF_AUTHORITY
    );

    private static final String REMAINDER = "other";
    // private static final RdfFormat RDF_OUTPUT_FORMAT = RdfFormat.NTRIPLES;
    private static final String TYPE_DEDUPING_PACKAGE = 
            "org.ld4l.bib2lod.rdfconversion.typededuping";
    
    
    public UriDeduper(String localNamespace, 
            String inputDir, String mainOutputDir) {
                 
        super(localNamespace, inputDir, mainOutputDir);

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
        
        Map<String, String> uniqueUris = new HashMap<String, String>();
        File[] inputFiles = new File(inputDir).listFiles();
        
        for ( File file : inputFiles ) {
            Map<String, String> dedupedUris = dedupeInputFile(file);
            if (dedupedUris != null) {
                uniqueUris.putAll(dedupedUris);
            }
        } 
        
        for ( File file : new File(inputDir).listFiles() ) {
            // Replace URIs using the uniqueUris map. Then remove duplicate 
            // lines created from this replacement.

            try {
                String filename = file.getName();
                LOGGER.trace("Start replacing lines in file " + filename);
                BufferedReader reader = Files.newBufferedReader(file.toPath());
                Set<String> uniqueLines = new LinkedHashSet<String>();             
                LineIterator lineIterator = new LineIterator(reader);
                while (lineIterator.hasNext()) {
                    String line = lineIterator.nextLine();
                    String processedLine = replaceUris(line, uniqueUris) + "\n";
                    /* Add the line to a set removes duplicate lines.
                     * NB Another way to replace duplicates would be to read 
                     * the lines into a Jena model, which also automatically 
                     * removes duplicate triples. We could compare performance 
                     * with the Set approach if that becomes an issue (though
                     * I would suspect that the Set would be faster than a 
                     * Model. Use of a model  has the advantage that the lines 
                     * don't have to be identical in terms of spaces.
                     */
                    uniqueLines.add(processedLine);
                }
                reader.close();
                
                String outputFilename =
                        FilenameUtils.getName(file.toString()); 
                File outputFile = new File(outputDir, outputFilename);
                PrintWriter writer = new PrintWriter(new BufferedWriter(
                        new FileWriter(outputFile, true)));  
                Iterator<String> setIterator = uniqueLines.iterator();
                while (setIterator.hasNext()) {
                    writer.append(setIterator.next());
                }
                writer.close();                
                LOGGER.trace("Done replacing lines in file " + file);                
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }       
        }

        LOGGER.trace("End process");
        return outputDir;
    }

    private Map<String, String> dedupeInputFile(File inputFile) {
        
        String filename = inputFile.getName();
        LOGGER.trace("Start processing file " + filename);
        
        String basename = FilenameUtils.getBaseName(inputFile.toString());
        OntologyType type = OntologyType.getByFilename(basename);
        if (type == null) {
            LOGGER.warn("No type found for file " + basename);
            return null;
        }
        LOGGER.debug("Type = " + type.toString());

        String className = 
                TYPE_DEDUPING_PACKAGE + "."
                + StringUtils.capitalize(type.namespace().prefix() 
                + type.localname() + "Deduper");
        
        try {
            Class<?> cls = Class.forName(className);

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
        } catch (ClassNotFoundException e) {
            LOGGER.warn("No class found for type " + type.toString());
            return null;
        }

        LOGGER.trace("Done processing file " + filename);
        return null;
    }

    private String replaceUris(String line, Map<String, String> uniqueUris) {          
        int size = uniqueUris.size();
        String[] originalUris = uniqueUris.keySet().toArray(new String[size]);
        String[] replacementUris = 
                uniqueUris.values().toArray(new String[size]);
        return StringUtils.replaceEach(line, originalUris, replacementUris);
    }

}