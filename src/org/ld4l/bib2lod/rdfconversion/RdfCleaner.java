package org.ld4l.bib2lod.rdfconversion;

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
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.LineIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;

public class RdfCleaner extends RdfProcessor {

    private static final Logger LOGGER = LogManager.getLogger(RdfCleaner.class);
    
    private static final Pattern URIS_TO_REPLACE = 
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
            Pattern.compile(
                    "(?<=<)http://[^>]+(?=>)|(?<!<)http://[^\"><]+(?!>)");
    
    
//    private static final Pattern DEC_CODE_PATTERN = 
//            Pattern.compile("&#(\\d+);");
    
    public RdfCleaner(String inputDir, String mainOutputDir) {            
        super(inputDir, mainOutputDir);
    }

    @Override
    public String process() {
        
        Instant processStart = Instant.now();
        LOGGER.info("Start RDF cleanup.");
        
        String outputDir = getOutputDir();
        
        // For logging
        int fileCount = 0;
        Instant fileStart = Instant.now();
       
        for ( File file : new File(inputDir).listFiles() ) {

            fileCount++;

            String filename = file.getName();
            // Skip directories and empty files (Jena chokes when reading an 
            // empty file into a model in later processors). Makes sense to 
            // clean them up here.
            if (file.isDirectory()) { 
                LOGGER.trace("Skipping directory " + filename);
                continue;
            }
            if (file.length() == 0) {
                LOGGER.trace("Skipping empty file " + filename);
                continue;
            }
            LOGGER.trace("Start processing file " + filename);
            replaceLinesInFile(file, outputDir); 
            
            if (fileCount == TimerUtils.NUM_FILES_TO_TIME) {
                LOGGER.info("Cleaned RDF in " + fileCount + " " 
                        + Bib2LodStringUtils.simplePlural("file", fileCount)
                        + ". " + TimerUtils.getDuration(fileStart));
                fileCount = 0;
                fileStart = Instant.now();   
            }
        }
        
        if (fileCount > 0) {
            LOGGER.info("Cleaned RDF in " + fileCount + " "
                    + Bib2LodStringUtils.simplePlural("file", fileCount)
                    + ". " + TimerUtils.getDuration(fileStart));    
        }   
        
        LOGGER.info("End RDF cleanup. "
                + TimerUtils.getDuration(processStart));
        
        return outputDir;
    }
    
    
    /*
     * Done as string replacement, because the illegal RDF handled by 
     * encodeURIs() can't be read into a Jena model without error.
     * 
     * However, removeStatementWithEmptyObject() could possibly be handled 
     * better within Jena. See notes for method, below.
     *
     */
    protected void replaceLinesInFile(File file, String outputDir) {
        BufferedReader reader;
        try {
            String filename = file.getName();
            LOGGER.trace("Start replacing lines in file " + filename);
            reader = Files.newBufferedReader(file.toPath());
            String outputFilename =
                    FilenameUtils.getName(file.toString()); 
            File outputFile = new File(outputDir, outputFilename);
            PrintWriter writer = new PrintWriter(new BufferedWriter(
                    new FileWriter(outputFile, true)));               
            LineIterator iterator = new LineIterator(reader);
            while (iterator.hasNext()) {
                String line = iterator.nextLine();
                // Remove empty lines
//                if (line.length() == 0) {
//                    LOGGER.trace("Removing empty line");
//                    continue;
//                }
                LOGGER.debug(line);
                String processedLine = processLine(line);
                if (LOGGER.isDebugEnabled()) {
                    // append newline before comparing lines?
                    if (!line.equals(processedLine)) {
                        LOGGER.debug("Original: " + line);
                        LOGGER.debug("New: " + processedLine);
                    }
                }
                writer.append(processedLine + "\n");
                if (processedLine.contains("<>")) {
                    LOGGER.debug("Writing out: " + processedLine);
                }
            }
            reader.close();
            writer.close();
            LOGGER.trace("Done replacing lines in file " + file);                
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       
    }
    
    private String processLine(String line) {
        line = removeStatementWithEmptyObject(line);
        if (! line.trim().isEmpty()) {
            line = encodeUris(line);
            
            // Any other operations go here
            
        } 
        
        return line;    
    }
  
    /*
     * Possibly could be handled better within Jena than as string search and
     * replace. Could move to BnodeConverter - rename to UriConverter?.
     * However, even within Jena different input serializations would have to
     * be handled differently, unless we convert all input to n-triples first. 
     * See sample input/output below. 
     * 
     * For now, just handling N-Triples, RDF/XML, and Turtle input as string 
     * replacements. JSON-LD input not supported; see notes below.
     * 
     * 
     * Sample input/output for different serializations on test input RDF
     * 
     * JSON-LD
     * 
     * Input:
     * 
     * "<http://www.loc.gov/mads/rdf/v1#isMemberOfMADSScheme>": 
     *     [
     *         { 
     *             "value" : "",
     *             "type" : "uri"
     *         }
     *     ], 
     *   
     *   
     * Output: 
     * 
     * Syntax errors reported for the jsonld file; not sure how to correct.
     * 
     * =================================================
     * 
     * N-TRIPLES
     * 
     * Input:
     * 
     * _:bnode131cornell72 <http://www.loc.gov/mads/rdf/v1#isMemberOfMADSScheme> <> . 
     * 
     * 
     * Output: 
     * 
     * Statement: [79815984c4052867d88535f9530d8039, http://www.loc.gov/mads/rdf/v1#isMemberOfMADSScheme, ]
     * Object URI: 
     * URI scheme: null
     * 
     * =================================================
     * 
     * RDF/XML
     * 
     * Input:
     * 
     * <madsrdf:Authority>
     *     <rdf:type rdf:resource="http://www.loc.gov/mads/rdf/v1#Topic"/>
     *     <madsrdf:authoritativeLabel>1894</madsrdf:authoritativeLabel>
     *     <madsrdf:isMemberOfMADSScheme rdf:resource=""/>
     * </madsrdf:Authority>
     * 
     * 
     * Output:
     * 
     * Statement: [-22417409:1511b56f653:-7fff, http://www.loc.gov/mads/rdf/v1#isMemberOfMADSScheme, file:///Users/rjy7/Workspace/jenatest/test-input/empty-object/72topic11.rdf]
     * Object URI: file:///Users/rjy7/Workspace/jenatest/test-input/empty-object/72topic11.rdf
     * URI scheme: file
     * 
     * =================================================
     * 
     * TURTLE
     * 
     * Input:
     * 
     * _:node1a4bq2o58x11 a madsrdf:Authority , madsrdf:Topic ;
     *     madsrdf:authoritativeLabel "1894" ;
     *     madsrdf:isMemberOfMADSScheme <unknown:namespace> .
     *   
     * 
     * Output:
     * 
     * Statement: [12bcf3f71ffa4bfac39729bfdb2b0d22, http://www.loc.gov/mads/rdf/v1#isMemberOfMADSScheme, unknown:namespace]
     * Resource object: unknown:namespace
     * URI scheme: unknown
     * 
     * NB Simply removing a line creates invalid Turtle output due to the 
     * resulting semi-colon rather than period at the end of the previous line:
     * 
     * _:node1a4bq2o58x11 a madsrdf:Authority , madsrdf:Topic ;
           madsrdf:authoritativeLabel "1894" ;
     *
     * In this case a Jena model would be a better implementation. So in Turtle
     * we don't modify this and let the <unknown:namespace> go through; it 
     * causes no problems in later processing stages.
     * 
     * =================================================
     * 
     */
    private String removeStatementWithEmptyObject(String line) {
        // ntriple input
        if (line.contains("<>") 
                // rdfxml input
                || line.contains("rdf:resource=\"\"")) {
                // turtle input; this doesn't work - see above
                // || line.contains("<unknown:namespace>")) {
            LOGGER.debug("Found line with empty object: " + line);
            line = "";
        }
        return line;
    }
    
    private String encodeUris(String line) {    
        StringBuilder sb = new StringBuilder(line);       
        Matcher m = URIS_TO_REPLACE.matcher(sb);
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
        return sb.toString();
    }


}
