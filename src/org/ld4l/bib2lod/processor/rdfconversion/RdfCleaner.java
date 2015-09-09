package org.ld4l.bib2lod.processor.rdfconversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.Processor;

public class RdfCleaner extends Processor {

    private static final Logger LOGGER = LogManager.getLogger(RdfCleaner.class);
    
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
                LOGGER.debug("output file: " + outputFile.toString());
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

    /*
     *  String content = "This is the content to write into file";
 PrintWriter out =new PrintWriter(new BufferedWriter(new FileWriter("Data.txt", true)));
 out.append(content);
 out.close();
     */
    private String processLine(String line) {
        //String newLine = null;
        
        //return newLine;
        return line;
    
    }



}
