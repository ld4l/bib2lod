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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfAgentDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfInstanceDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfResourceDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfTopicDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfWorkDeduper;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.DeduperFactory;

// May need to be abstract - we only instantiate PersonDeduper, WorkDeduper, etc.
public class ResourceDeduper extends RdfProcessor {

    private static final Logger LOGGER = LogManager.getLogger(ResourceDeduper.class);
    
  
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
            OntType.BF_EVENT,
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

    private static final String REMAINDER_FILENAME = "other";
    private static final String NEW_STATEMENT_FILENAME = "newStatements";
    // private static final RdfFormat RDF_OUTPUT_FORMAT = RdfFormat.NTRIPLES;
    
    public ResourceDeduper(String localNamespace, 
            String inputDir, String mainOutputDir) {
                 
        super(localNamespace, inputDir, mainOutputDir);   
    }
    
    protected static List<OntType> getTypesToDedupe() {
        return TYPES_TO_DEDUPE; 
    }
    
    protected static String getRemainderFilename() {
        return REMAINDER_FILENAME;
    }

//    @Override
//    protected RdfFormat getRdfOutputFormat() {
//        return RDF_OUTPUT_FORMAT;
//    }
    
    @Override
    public String process() {
        LOGGER.info("Start process");
        
        String outputDir = getOutputDir();    

        Map<String, String> uniqueUris = new HashMap<String, String>();
        Model newStatements = ModelFactory.createDefaultModel();

        File[] inputFiles = new File(inputDir).listFiles();
        
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
//      for ( OntType type : TYPES_TO_DEDUPE ) {
//      BfResourceDeduper deduper = 
//              DeduperFactory.createBfResourceDeduper(type, inputDir);
//      if (deduper == null) {
//          LOGGER.debug("No deduper found for type " + type);
//          continue;
//      }
//      Map<String, String> dedupedUris = deduper.dedupe();
//      if (dedupedUris != null) {
//          uniqueUris.putAll(dedupedUris);
//      }
//      
//      Model statements = deduper.getNewStatements();
//      if (statements != null) {
//          newStatements.add(statements);
//      }
//  }
        
        getUniqueUris(inputFiles, uniqueUris, newStatements);
        dedupeUris(inputFiles, uniqueUris, outputDir);
        writeNewStatements(newStatements);

        LOGGER.info("End process");
        return outputDir;
    }


    private void getUniqueUris(File[] inputFiles, 
            Map<String, String> uniqueUris, Model newStatements) {

        for ( File file : inputFiles ) {
            String filename = file.getName();
            LOGGER.debug("Deduping file " + filename);
            BfResourceDeduper deduper = 
                    DeduperFactory.createBfResourceDeduper(file);
            if (deduper == null) {
                LOGGER.debug("No deduper found for file " + filename);
                continue;
            }
            Model model = readModelFromFile(file);
            Map<String, String> dedupedUris = deduper.dedupe(model);
            if (dedupedUris != null) {
                uniqueUris.putAll(dedupedUris);
            }
            Model statements = deduper.getNewStatements();
            if (statements != null) {
                newStatements.add(statements);
            }
        }         
    }
    
    private void dedupeUris(File[] inputFiles, Map<String, String> uniqueUris,
            String outputDir) {

        for ( File file : inputFiles ) {
            // Replace URIs using the uniqueUris map. Then remove duplicate 
            // lines created from this replacement.
            try {
                LOGGER.info("Replacing lines in file " + file.getName());
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
                     * Model). Use of a model  has the advantage that the lines 
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
                LOGGER.info("Done replacing lines in file " + file);   
                               
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }       
        }        
    }
   
    private void writeNewStatements(Model newStatements) {
        // Assumes there aren't too many of these to hold all in memory. If too
        // many, append to file after each iteration through the loop.
        if (!newStatements.isEmpty()) {
            writeModelToFile(newStatements, NEW_STATEMENT_FILENAME);
        }
    }
    
    private String replaceUris(String line, Map<String, String> uniqueUris) {          
        int size = uniqueUris.size();
        String[] originalUris = uniqueUris.keySet().toArray(new String[size]);
        String[] replacementUris = 
                uniqueUris.values().toArray(new String[size]);
        return StringUtils.replaceEach(line, originalUris, replacementUris);
    }

}