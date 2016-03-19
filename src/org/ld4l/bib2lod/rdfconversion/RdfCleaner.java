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
import java.util.Arrays;
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
    
    // ntriples, turtle
    private static final Pattern URI_BRACKETED = 
            Pattern.compile("(?<=<)http://[^>]+(?=>)");  
    
    private static final Pattern URI_RDFXML = 
            Pattern.compile("(?<==\")http://[^\"]+(?=\")");
    
//    private static final Pattern URI = 
//            /*
//             * Matches:
//             * <http://id.loc.gov/vocabulary/organizations/*cleveland st univ lib*>
//             * <http://id.loc.gov/vocabulary/geographicAreas/f-tz---\>
//             * <http://id.loc.gov/authorities/classification/TN"69>
//             * "http://id.loc.gov/authorities/classification/TN&#34;69"
//             * 
//             * Does not match:
//             * "http://id.loc.gov/vocabulary/subjectSchemes/fast" . 
//             * 
//             * Does not break: 
//             * <http://id.loc.gov/authorities/classification/TN"69> at the "
//             * 
//             * Does not match:
//             * http://www.organic-europe.net"
//             * The quote is the close of a quoted string literal value
//             * 
//             * Does not match: 
//             * http://www.organic-europe.net / Stefanie Graf and Helga Willer
//             * Part of a string literal value
//             */
//            
//            Pattern.compile(                   
//                    // Bracketed URI (nt, ttl, json): no internal closing 
//                    // brackets
//                    "(?<=<)http://[^>]+(?=>)" 
//                    // Quoted URI (rdfxml): no internal quotes 
//                    // Require quotes on both ends, otherwise the quote is the
//                    // opening or closing of a quoted string.
//                    + "|(?<=\")http://[^\"]+(?=\")"
//                    );
    
//    private static final Pattern DEC_CODE_PATTERN = 
//            Pattern.compile("&#(\\d+);");
    
    private final Pattern BAD_LOCALNAME = 
            Pattern.compile("(" + localNamespace + ")(\\d)");
    
//    private final Pattern BNODE_ID = 
//            Pattern.compile("_:bnode\\d+");
    
    
    public RdfCleaner(
            String localNamespace, String inputDir, String mainOutputDir) {            
        super(localNamespace, inputDir, mainOutputDir);
    }

    @Override
    public String process() {
        
        Instant processStart = Instant.now();
        LOGGER.info("START RDF cleanup.");
        
        String outputDir = getOutputDir();
        
        File[] inputFiles = new File(inputDir).listFiles();
        int totalFileCount = inputFiles.length;
        
        // For consistent ordering. Helps locate errors if program exits
        // unexpectedly. Time to sort is miniscule (0.008 seconds on 34,540 
        // files).
        Arrays.sort(inputFiles);
//        LOGGER.info("Sorted " + Bib2LodStringUtils.count(totalFileCount, "file") 
//                + ". Duration: " + TimerUtils.getDuration(processStart) + "."); 
                                       
        int fileCount = 0;
        for ( File file : inputFiles ) {
            
            Instant fileStartTime = Instant.now();
            fileCount++;  
            
            String filename = file.getName();

            // Skip directories and empty files (Jena chokes when reading an 
            // empty file into a model in later processors). Makes sense to 
            // clean them up here.
            if (file.isDirectory()) { 
                LOGGER.trace(
                        "Skipping " + filename + " because it is a directory.");
                continue;
            }
            if (file.length() == 0) {
                LOGGER.trace("Skipping " + filename + " because it is empty.");
                continue;
            }
                        
            LOGGER.info("Start RDF cleanup in file " + filename 
                    + " (file " + fileCount + " of " + totalFileCount  
                    + " input "
                    + Bib2LodStringUtils.count(totalFileCount, "file")
                    + ").");
            
            replaceLinesInFile(file, outputDir); 
            
            LOGGER.info("End RDF cleanup in file " + filename + " (file "
                    + fileCount + " of " 
                    + Bib2LodStringUtils.count(totalFileCount, " input file")  
                    + " = " 
                    + TimerUtils.percent(fileCount, totalFileCount) + "%). "
                    + "). Duration: " + TimerUtils.getDuration(fileStartTime)
                    + ".");
        }
        
        LOGGER.info("END RDF cleanup in total of "                
                + Bib2LodStringUtils.count(totalFileCount, "input file")
                + ". Duration: " + TimerUtils.getDuration(processStart) + ".");
        
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
         
        String fileExt = FilenameUtils.getExtension(file.getName());
        
        /*
         * By using the URI pattern that corresponds to the RDF serialization
         * type, we eliminate URIs that appear as part of string literals,
         * either within quotes or not. We also use =" lookbehind in rdfxml
         * URIs for the same reason.
         * This means we do not correct URI string literals at all, as in these
         * hypothetical examples:
         * rdfxml: <bf:authoritySource>http://id.loc.gov/vocabulary/ subjectSchemes/fast</bf:authoritySource>
         * nt: <http://draft.ld4l.org/cornell/21384topic15> <http://bibframe.org/vocab/authoritySource> "http://id.loc.gov/vocabulary/ subjectSchemes/fast" . 
         * ttl: <http://draft.ld4l.org/cornell/21384topic15> bf:authoritySource "http://id.loc.gov/vocabulary/ subjectSchemes/fast"
         * It's tricky to distinguish this case from those where URI is part of 
         * a larger string literal, where it's more difficult to  isolate a 
         * faulty URI from surrounding text. For now we'll say we only correct 
         * bad data if it breaks later processing.
         */
        Pattern uriPattern = 
                fileExt.equals("rdf") ? URI_RDFXML : URI_BRACKETED;
        
//        LOGGER.debug("Using uri pattern " + uriPattern.toString() 
//                + " for file extension " + fileExt);
        
        String outputFilename =
                FilenameUtils.getName(file.toString()); 
        
        File outputFile = new File(outputDir, outputFilename);
        
        BufferedReader reader;
        try {           
            reader = Files.newBufferedReader(file.toPath());
      
            PrintWriter writer = new PrintWriter(new BufferedWriter(
                    new FileWriter(outputFile, true)));     
            
            LineIterator iterator = new LineIterator(reader);
            
            while (iterator.hasNext()) {
                String line = iterator.nextLine();

                String processedLine = processLine(line, uriPattern);
                
                if (LOGGER.isDebugEnabled()) {
                    // append newline before comparing lines?
                    if (!line.equals(processedLine)) {
                        // LOGGER.debug("Original: " + line);
                        // LOGGER.debug("New: " + processedLine);
                    }
                }
                
                writer.append(processedLine + "\n");
                
                if (LOGGER.isDebugEnabled()) {
                    if (processedLine.contains("<>")) {
                        // LOGGER.debug("Writing out: " + processedLine);
                    }
                }
            }
            
            reader.close();
            writer.close();
              
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }       
    }
    
    private String processLine(String line, Pattern uriPattern) {

        line = removeStatementWithEmptyObject(line);
        if (line.trim().isEmpty()) {
            return line;
        }

        line = encodeUris(line, uriPattern);
        
        line = fixLocalNames(line);
        
        /* 
         * Can't do bnode conversion here: we don't get bnode ids in rdf/xml
         * input. Could convert all formats to ntriples before starting 
         * RDF cleanup, but that's an extra step as well. Possible future
         * performance optimization if needed, and if it IS a performance
         * improvement.
         * 
         * line = convertBnodes(line, bnodesToUris);
         */
        
        return line;    
    }
  


    /*
     * Possibly could be handled better within Jena than as string search and
     * replace. Could move to BnodeConverter1 - rename to UriConverter?.
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
                // turtle input; this doesn't work - see comments above
                // || line.contains("<unknown:namespace>")) {
            // LOGGER.debug("Found line with empty object: " + line);
            line = "";
        }
        return line;
    }
    
    private String encodeUris(String line, Pattern uriPattern) {  
        

        StringBuilder sb = new StringBuilder(line);   

        // LOGGER.debug("Processing line: " + line);

        Matcher m = uriPattern.matcher(sb);
        
//        if (LOGGER.isDebugEnabled()) {
//            if (!m.find()) {
//                LOGGER.debug("No match found.");
//            }
//        }
        
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

                String uri = new URI(
                        url.getProtocol(), 
                        url.getUserInfo(), 
                        url.getHost().replaceAll(" ", ""), 
                        url.getPort(), 
                        url.getPath(), 
                        url.getQuery(), 
                        url.getRef())
                        .toString();
                
                /*
                 * &#34; must be replaced manually, because the URI constructor
                 * doesn't change it, and Jena will not accept it when reading
                 * a file into a model. There may be others here, in which case
                 * we can replace decimal with hex codes using DEC_CODE_PATTERN
                 * above.
                 */
                uri = uri.replace("&#34;", "%22");
                
                if (! uri.equals(m.group().toString())) {
                    
//                    LOGGER.debug("Replacing original line with new line: " 
//                            + uri);
                    sb.replace(m.start(), m.end(), uri);
                    // Manually reset matchPointer, since the old and new 
                    // strings may differ in length.
                    matchPointer += uri.length() - match.length();
                    
                } else {
                    // LOGGER.debug("No change");
                }
                
            // Deletes an entire string literal containing a bad URI, but these
            // are very rare.
            } catch (MalformedURLException e) {
//                LOGGER.debug("MalformedURLException in line \"" + line 
//                        + "\". Deleting line.");
                return "";
            } catch (URISyntaxException e) {
//                LOGGER.debug("URISyntaxException in line \"" + line 
//                        + "\". Deleting line.");
                return "";
            }
        }   
        
        return sb.toString();
    }

    /* Bibframe generates local names with an initial digit, which is
     * incompatible with Jena's Resource.getLocalName() method. Jena's 
     * expectation that a local name not start with a digit is presumably 
     * based on the XML spec, which doesn't allow it. That would only apply to
     * RDF/XML, but Jena applies it across the board. Fixing it means that the
     * generated LD4L RDF will be valid in RDF/XML as well as in other 
     * serializations.
     */
    protected String fixLocalNames(String line) {
        
        String alphaPrefix = RdfProcessor.getLocalNameAlphaPrefix();
        
        StringBuilder sb = new StringBuilder(line);
        LOGGER.debug(line);
        Matcher m = BAD_LOCALNAME.matcher(sb);
        int matchPointer = 0;
        if (LOGGER.isDebugEnabled()) {
            if (!m.find(matchPointer)) {
                LOGGER.debug("No local name starting with digit found.");
            }
        }
        while (m.find(matchPointer)) {
            LOGGER.debug("Match: " + m.group(1) + " " + m.group(2));
            sb.replace(m.start(), m.end(),
                     m.group(1) + alphaPrefix + m.group(2));
            matchPointer = m.end() + 1;
        }     
        LOGGER.debug("Returning from fixLocalNames(): " + sb.toString());
        return sb.toString();     
    } 

}
