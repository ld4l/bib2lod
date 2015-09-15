package org.ld4l.bib2lod.processor.rdfconversion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
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
            /*
             * Matches:
             * <http://id.loc.gov/vocabulary/organizations/*cleveland st univ lib*>
             * <http://id.loc.gov/vocabulary/geographicAreas/f-tz---\>
             * <http://id.loc.gov/authorities/classification/TN"69>
             * "http://id.loc.gov/authorities/classification/TN&#34;69"
             * 
             * Does not match:
             * "http://id.loc.gov/vocabulary/subjectSchemes/fast" . 
             * 
             * Does not break 
             * <http://id.loc.gov/authorities/classification/TN"69> into
             * http://id.loc.gov/authorities/classification/TN"
             * 
             * The disjunction in the regex is required to handle all these 
             * cases: If we remove the lookaheads/lookbehinds, then we must
             * exclude " internal to the uri, so that we don't extend past
             * the end of a quoted uri. But then we break up 
             * <http://id.loc.gov/authorities/classification/TN"69> after the
             * double quote. So here we are saying we have either a bracketed
             * uri which cannot contain a bracket in the middle (but could 
             * contain a quote), or a non-bracketed uri (quoted or not) which
             * cannot contain internal brackets or quotes. A quoted uri in 
             * rdfxml escapes an actual quote as &#34;
             */
            Pattern.compile("(?<=<)http://[^>]+(?=>)|(?<!<)http://[^\"><]+(?!>)");
    
//    private static final Pattern DEC_CODE_PATTERN = 
//            Pattern.compile("&#(\\d+);");
    
    public RdfCleaner(String localNamespace, String inputDir,
            String mainOutputDir) {
        super(localNamespace, inputDir, mainOutputDir);
    }

    @Override
    public String process() {
        
        LOGGER.trace("Start process");
        String outputDir = createOutputDir();
        for ( File file : new File(inputDir).listFiles() ) {
            String filename = file.getName();
            // Skip directories and empty files (Jena chokes when reading an 
            // empty file into a model). Makes sense to clean them up here.
            if (file.isDirectory()) { 
                LOGGER.trace("Skipping directory " + filename);
                continue;
            }
            if (file.length() == 0) {
                LOGGER.trace("Skipping empty file " + filename);
                continue;
            }
            LOGGER.trace("Start processing file " + filename);
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
                    // Remove empty lines
                    if (line.length() == 0) {
                        LOGGER.trace("Removing empty line");
                        continue;
                    }
                    // LOGGER.debug("Original: " + line);
                    String processedLine = processLine(line);
                    // LOGGER.debug("New: " + newLine);
                    writer.append(processedLine + "\n");
                }
                reader.close();
                writer.close();
                LOGGER.trace("Done processing file " + filename);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
   
        }
        LOGGER.trace("End process");
        return outputDir;
    }

    private String processLine(String line) {
        line = encodeUris(line);
        // Any other operations go here
        return line;
    
    }
    
    private String encodeUris(String line) {
        // LOGGER.debug("Original line: " + line);     
        StringBuilder sb = new StringBuilder(line);       
        Matcher m = URI_PATTERN.matcher(sb);
        int matchPointer = 0;
        while (m.find(matchPointer)) {         
            try {
                matchPointer = m.end();
                String match = m.group();
                /*
                 * Only the multi-argument URI constructor encodes illegal
                 * characters, so use URL methods to break up the string into
                 * components to feed to the URI constructor.
                 */
                URL url = new URL(match);
                String uri = new URI(url.getProtocol(), url.getUserInfo(), 
                        url.getHost(), url.getPort(), url.getPath(), 
                        url.getQuery(), url.getRef()).toString();
                /*
                 * &#34; must be replaced manually, because the URI constructor
                 * doesn't change it, and Jena will not accept it when reading
                 * a file into a model. There may be others here, in which case
                 * we can replace decimal with hex codes using DEC_CODE_PATTERN
                 * above.
                 */
                uri = uri.replace("&#34;", "%22");
                if (! uri.equals(m.group().toString())) {
                    sb.replace(m.start(), m.end(), uri);
                    // LOGGER.debug("original uri: " + match);
                    // LOGGER.debug("encoded uri: " + uri);
                    // Manually reset matchPointer, since the old and new 
                    // strings may differ in length.
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
        return sb.toString();
    }

}
