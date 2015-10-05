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
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.reasoner.rulesys.builtins.Print;
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
    private static final List<OntType> TYPES_TO_DEDUPE = Arrays.asList(
            OntType.BF_PERSON,
            OntType.BF_FAMILY,
            // NB There are no instances of this type in the entire CUL catalog.
            OntType.BF_JURISDICTION,
            OntType.BF_MEETING,
            OntType.BF_ORGANIZATION,
            OntType.BF_WORK,  
            OntType.BF_INSTANCE,
            OntType.BF_PLACE,
            OntType.BF_TOPIC
    );

    private static final String REMAINDER = "other";
    // private static final RdfFormat RDF_OUTPUT_FORMAT = RdfFormat.NTRIPLES;
    
    private Model newStatements;
    private Map<String, String> uniqueUris;
    
    public UriDeduper(String localNamespace, 
            String inputDir, String mainOutputDir) {
                 
        super(localNamespace, inputDir, mainOutputDir);
        
        newStatements = ModelFactory.createDefaultModel();
        uniqueUris = new HashMap<String, String>();
        
    }
    
    protected static List<OntType> getTypesToDedupe() {
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
        
        String outputDir = getOutputDir();        

        File[] inputFiles = new File(inputDir).listFiles();
        
        for ( File file : inputFiles ) {
            dedupeInputFile(file);
//            if (dedupedUris != null) {
//                uniqueUris.putAll(dedupedUris);
//            }
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
                    /* Adding the line to a set removes duplicate lines.
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
        
        // Assumes there aren't too many of these to hold all in memory. If too
        // many, append to file after each iteration through the loop.
        if (!newStatements.isEmpty()) {
            writeModelToFile(newStatements, "newStatements");
        }

        LOGGER.trace("End process");
        return outputDir;
    }

    private void dedupeInputFile(File inputFile) {
        
        String filename = inputFile.getName();
        LOGGER.trace("Start processing file " + filename);
        
        String basename = FilenameUtils.getBaseName(inputFile.toString());
        OntType type = OntType.getByFilename(basename);
        if (type == null) {
            LOGGER.warn("No type found for file " + basename);
            return;
        }
        LOGGER.debug("Type = " + type.toString());

        try {
            Class<?> cls = type.deduper();
            if (cls == null) {
                LOGGER.debug(
                        "No deduper class found for type " + type.toString());
                return;
            }

            TypeDeduper deduper = (TypeDeduper) cls.newInstance();

            Model model = readModelFromFile(inputFile);
            

            Map<String, String> dedupedUris = deduper.dedupe(type, model);
            if (dedupedUris != null) {
                uniqueUris.putAll(dedupedUris);
            }
            
            Model statements = deduper.getNewStatements();
            if (statements != null) {
                newStatements.add(statements);
            }
            
            
        } catch (InstantiationException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        LOGGER.trace("Done processing file " + filename);
    }

    private String replaceUris(String line, Map<String, String> uniqueUris) {          
        int size = uniqueUris.size();
        String[] originalUris = uniqueUris.keySet().toArray(new String[size]);
        String[] replacementUris = 
                uniqueUris.values().toArray(new String[size]);
        return StringUtils.replaceEach(line, originalUris, replacementUris);
    }

}