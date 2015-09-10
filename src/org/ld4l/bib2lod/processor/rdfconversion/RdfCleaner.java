package org.ld4l.bib2lod.processor.rdfconversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
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
    private static final Pattern URI_PATTERN = 
            Pattern.compile("(?<=<)http://[^>]+(?=>)|(?<!<)http://[^\"><]+(?!>)");
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
        line = encodeUris(line);
        // Any other operations go here
        return line;
    
    }
    
    private String encodeUris(String line) {
        // LOGGER.debug("Original line: " + line);     
        StringBuilder text = new StringBuilder(line);       
        Matcher m = URI_PATTERN.matcher(text);
        int matchPointer = 0;
        while (m.find(matchPointer)) {
            matchPointer = m.end();
            try {
                String match = m.group();
                URL url = new URL(match);
                // Only the multi-argument URI constructor encodes illegal
                // characters, so use URL methods to break up the string into
                // components to feed to the URI constructor.
                String uri = new URI(url.getProtocol(), url.getUserInfo(), 
                        url.getHost(), url.getPort(), url.getPath(), 
                        url.getQuery(), url.getRef()).toString();
                uri = uri.replace("&#34;", "%22");
                if (! uri.equals(m.group().toString())) {
                    text.replace(m.start(), m.end(), uri);
                    LOGGER.debug("Encoding illegal characters:");
                    LOGGER.debug(match);
                    LOGGER.debug("uri = " + uri);
                    matchPointer += uri.length() - match.length();
                }
            } catch (MalformedURLException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            } catch (URISyntaxException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        // LOGGER.debug("New line: " + text.toString());
        return text.toString();
    }

}
