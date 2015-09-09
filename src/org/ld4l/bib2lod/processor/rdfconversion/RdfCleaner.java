package org.ld4l.bib2lod.processor.rdfconversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.Processor;

public class RdfCleaner extends Processor {

    private static final Logger LOGGER = LogManager.getLogger(RdfCleaner.class);
    private static final Pattern URI_PATTERN = Pattern.compile("[<\"]http://[^\">]+");
    
    public RdfCleaner(String localNamespace, String inputDir,
            String mainOutputDir) {
        super(localNamespace, inputDir, mainOutputDir);

    }

    @Override
    public String process() {
        
        String outputDir = createOutputDir();
        for ( File file : new File(inputDir).listFiles() ) {
            try {
                BufferedReader reader = Files.newBufferedReader(file.toPath());
                String outputFilename =
                        FilenameUtils.getName(file.toString()); 
                File outputFile = new File(outputDir, outputFilename);
                PrintWriter writer = new PrintWriter(new BufferedWriter(
                        new FileWriter(outputFile, true)));               
                LineIterator iterator = new LineIterator(reader);
                while (iterator.hasNext()) {
                    String line = iterator.nextLine();
                    // LOGGER.debug("Original: " + line);
                    String newLine = processLine(line);
                    // LOGGER.debug("New: " + newLine);
                    writer.append(newLine + "\n");
                }
                reader.close();
                writer.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        
        }
        return outputDir;
    }

    private String processLine(String line) {
        // LOGGER.debug("original: " + line);
        line = fixUris(line);
        // LOGGER.debug("new: " + line);
        return line;
    
    }
    
    private String fixUris(String line) {

        Matcher m = URI_PATTERN.matcher(line);
        StringBuffer result = new StringBuffer();
        while (m.find()) {
            /* Could do a uri encoding here. Then the new string will be a
             * different length from the original. Use technique described in
             * Matching Regular Expressions p.383.
             */
            String replacement = m.group().replaceAll(" ", "_");
            m.appendReplacement(result, replacement);
        }
        m.appendTail(result);
        return result.toString();
    }



}
