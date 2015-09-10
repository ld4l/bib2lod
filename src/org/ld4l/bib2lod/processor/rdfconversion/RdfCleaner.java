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
                /*
                 * Only the multi-argument URI constructor encodes illegal
                 * characters, so use URL methods to break up the string into
                 * components to feed to the URI constructor.
                 */
                String uri = new URI(url.getProtocol(), url.getUserInfo(), 
                        url.getHost(), url.getPort(), url.getPath(), 
                        url.getQuery(), url.getRef()).toString();
                /*
                 * &#34; must be replaced manually, because the URI constructor
                 * doesn't change it, but Jena will not accept it when reading
                 * a file into a model. There may be others here, in which case
                 * we can use a hash to replace decimal with hex codes.
                 */
                uri = uri.replace("&#34;", "%22");
                if (! uri.equals(m.group().toString())) {
                    text.replace(m.start(), m.end(), uri);
                    // LOGGER.debug("Encoding illegal characters:");
                    // LOGGER.debug(match);
                    // LOGGER.debug("uri = " + uri);
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
