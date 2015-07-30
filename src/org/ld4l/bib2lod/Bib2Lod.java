package org.ld4l.bib2lod;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.processor.Processor;


public class Bib2Lod {
    
    private static final Logger logger = LogManager.getLogger(Bib2Lod.class);
    private static final List<String> validActions = 
            new ArrayList<String>(Arrays.asList("dedupe"));
    

    /** 
     * Read in program options and call appropriate processing functionality.
     * @param args
     */
    public static void main(String[] args) {

        // Define program options
        Options options = getOptions();
        
        // Get commandline options
        CommandLine cmd = getCommandLine(options, args);
        if (cmd == null) {
            return;
        }
        
        // Process commandline arguments and exit if any are invalid.
        String namespace = getValidNamespace(cmd.getOptionValue("namespace"));
        if (namespace == null) {
            return;
        }

        List<String> actions = getValidActions(cmd.getOptionValues("actions"));
        if (actions == null) {
            return;
        }
        
        File inDir = getInputDirectory(cmd.getOptionValue("indir"));
        if (inDir == null) {
            return;
        }
        
        File outDir = createOutputDirectory(cmd.getOptionValue("outdir"));
        if (outDir == null) {
            return;
        }

        String format = cmd.getOptionValue("format", "rdfxml");
               
        Processor processor = new Processor(namespace, format, inDir, outDir);
        
        processor.process(actions);
        
        
//         
//        List<InputStream> records = readFiles(readdir);
//        
//        // Process the RDF input files and receive a union model for writing.
//        RDFPostProcessor p = new RDFPostProcessor(localNamespace);
//        Model allRecords = p.processRecords(records);
//        
//        // Write out the union model to a file.
//        try {
//            String outfile = getOutputFilename(outdir);
//            OutputStream out = new FileOutputStream(outfile);
//            // Second param = RDF serialization. We may want to be able to 
//            // specify this on the commandline. For now we use the default
//            // RDF/XML.
//            allRecords.write(out, null);
//            out.close();
//        } catch (IOException e) {
//            // TODO Auto-generated catch block
//            e.printStackTrace();
//        }    
//        
          logger.info("Done!");
    }

    /**
     * Validate actions.
     * Rudimentary validation - checks only for invalid actions. Doesn't check
     * for invalid combinations, etc.
     * @param actions
     * @return
     */
    private static List<String> getValidActions(String[] selectedActions) {
        List<String> actions = new ArrayList<String>(Arrays.asList(selectedActions));
        if (actions.retainAll(validActions)) {
            if (actions.size() == 0) {
                logger.fatal("No valid actions specified.");
                return null;
            } else {
                logger.error("Invalid actions removed.");
            }
        }
        return actions;
    }


    /**
     * Check for valid input directory. Return the input directory if it exists,
     * otherwise log an error and return null. 
     * @param path - absolute or relative path to input directory
     * @return the input directory if it exists, otherwise null
     */
    private static File getInputDirectory(String path) {

        File inDir = new File(path);
        if (!inDir.isDirectory()) {
            try {
                logger.fatal("Input directory " + inDir.getCanonicalPath() 
                        + " does not exist or is not a directory.");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }
        
        return inDir;
    }
    
    /**
     * Make output directory and any intermediate directories. Return the 
     * output directory if it was successfully created, otherwise log an error
     * and return null.
     * @param path - absolute or relative path to output directory. A child
     * directory named with current datetime will be created under it.
     * @return the output directory if it was successfully created, otherwise 
     * null
     */
    private static File createOutputDirectory(String path) {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
        Date date = new Date();
        String now = dateFormat.format(date);
        File outDir = new File(path, now);
        if (! outDir.mkdirs()) {
            try {
                String outPath = outDir.getCanonicalPath();
                logger.error("Cannot create output directory " + outPath + ".");
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        } 
        
        return outDir;
    }

        
    /**
     * Check for valid namespace within http scheme. Return the namespace if
     * valid, otherwise log an error and return null. 
     * @param namespace
     * @return valid namespace or null
     */
    private static String getValidNamespace(String namespace) {

        String[] schemes = {"http"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (!urlValidator.isValid(namespace)) {
            logger.fatal("Valid HTTP namespace required.");
            return null;
        }
        
        return namespace;
    }


    /**
     * Print help text.
     * @param options
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.setWidth(80);
        formatter.printHelp("bib2lod", options, true);
    }


    /**
     * Parse commandline options.
     * @param options
     * @param args
     * @return
     */
    private static CommandLine getCommandLine(Options options, String[] args) {
        
        // Parse program arguments
        CommandLineParser parser = new DefaultParser();
        try {
            return parser.parse(options, args);
        } catch (MissingOptionException e) {
            logger.fatal(e.getMessage());
            printHelp(options);
        } catch (UnrecognizedOptionException e) {
            logger.fatal(e.getMessage());
            printHelp(options);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            logger.fatal(e.getStackTrace().toString());
        }
        return null;
    }
    

    /**
     * Convert a directory of RDF files into a list of input streams for
     * processing.
     * @param directory - the directory
     * @return List<InputStream>
     */
//    private static List<InputStream> readFiles(File directory) {
//        // Convert the directory of RDF files into a list of input streams
//        // for processing.
//
//        // Despite the name, lists both files and directories.
//        File[] items = directory.listFiles();
//        List<InputStream> records = new ArrayList<InputStream>();
//    
//        for (File file : items) {
//            if (file.isFile()) {
//                try {
//                    records.add(new FileInputStream(file));
//                } catch (FileNotFoundException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }  
//        return records;
//    }
    
    /**
     * Define the commandline options accepted by the program.
     * @return an Options object
     */
    private static Options getOptions() {
        
        Options options = new Options();

        Option inputOption = Option.builder("i")
                .longOpt("indir")
                .required()
                .hasArg()
                .desc("absolute or relative path to input files")
                .build();
        options.addOption(inputOption);
        
        Option outputOption = Option.builder("o")
                .longOpt("outdir")
                .required()
                .hasArg()
                .desc("absolute or relative path to output directory")
                .build();
        options.addOption(outputOption);
        
        Option namespaceOption = Option.builder("n")
                .longOpt("namespace")
                .required()
                .hasArg()
                .desc("Local HTTP namespace for minting and deduping URIs")
                .build();
        options.addOption(namespaceOption);
        
        Option formatOption = Option.builder("f")
                .longOpt("format")
                .required(false)
                .hasArg()
                .desc("RDF serialization format of input and output. Valid "
                        + "options: nt, rdfxml. Defaults to rdfxml.")
                .build();
        options.addOption(formatOption);      

        // For now the only defined action is "dedupe". Will add others later:
        // conversion to ld4l ontology, entity resolution, etc.
        Option actions = Option.builder("a")
                .longOpt("actions")
                .required()
                .hasArg()
                .desc("Processing actions. Valid values: " + StringUtils.join(validActions, ", ") + ".")
                .argName("dedupe")
                .build();
        options.addOption(actions);
        
        return options;
    }


    /**
     * Convert a directory of RDF files into a list of input streams for
     * processing.
     * @param readdir - path to the directory
     * @return List<InputStream>
     */
//    private static List<InputStream> readFiles(String readdir) {
//        File directory = new File(readdir);
//        return readFiles(directory);
//    }
    
//    private static String getOutputFilename(String outdir) {
//        
//        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
//        Date date = new Date();
//        String now = dateFormat.format(date);
//        // Currently allowing only RDF/XML output. Later may offer 
//        // serialization as a program argument. This will affect extension
//        // as well as second parameter to allRecords.write(out, null) above.
//        String ext = "rdf";
//        String filename = "out." + now.toString() + "." + ext;
//        Path path = Paths.get(outdir, filename);   
//        return path.toString();
//    }
        
}


