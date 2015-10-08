package org.ld4l.bib2lod;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Bib2Lod {
    
    private static final Logger LOGGER = LogManager.getLogger(Bib2Lod.class);  
    private static final List<String> VALID_ACTIONS = Action.validActions();
//    private static final List<RdfFormat> VALID_RDF_OUTPUT_FORMATS = 
//            RdfFormat.allFormats();          
//    private static final RdfFormat DEFAULT_RDF_OUTPUT_FORMAT = 
//            RdfFormat.NTRIPLES;            
            
    
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
        String namespace = cmd.getOptionValue("namespace");
        if (!isValidNamespace(namespace)) {
            return;
        }

        // RdfFormat format = getValidOutputFormat(cmd.getOptionValue("format"));
        
        Set<Action> actions = getValidActions(cmd.getOptionValues("actions"));
        if (actions == null) {
            LOGGER.debug("No valid actions specified.");
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            for (Action a : actions) {
                LOGGER.debug(a.label());
            }
        }

        String absInputDir = getAbsoluteInputDir(
                cmd.getOptionValue("indir"));
        if (absInputDir == null) {
            return;
        }
        
        String absTopLevelOutputDir = 
                createTopLevelOutputDir(cmd.getOptionValue("outdir"));
        if (absTopLevelOutputDir == null) {
            return;
        }
               
        ProcessController processController = new ProcessController(namespace, 
                absInputDir, absTopLevelOutputDir); 
        String absFinalOutputDir = processController.processAll(actions);
        if (absFinalOutputDir == null) {
            LOGGER.info("Processing failed.");
        } else {
            LOGGER.info("Done! Results in " + absFinalOutputDir);
        }

    }


    /**
     * Validate actions.
     * Removes invalid actions and adds missing prerequisites.
     * @param actions
     * @return
     */
    private static Set<Action> getValidActions(String[] selectedActions) {

        EnumSet<Action> actions = EnumSet.noneOf(Action.class);
        
        // Find the Action associated with the commandline option, and remove 
        // any invalid actions
        for (String selectedAction : selectedActions) {
            Action action = Action.get(selectedAction);
            if (action != null) {
                actions.add(action);
            } else {
                LOGGER.warn("Invalid action '" + selectedAction + "' removed.");
            }
            
        }
        
        // Add prerequisites of each requested action.
        // Clone actions to prevent modifying inside the loop
        EnumSet<Action> allActions = actions.clone();
        for (Action action : actions) {
            boolean addedNewActions = 
                    allActions.addAll(action.recursivePrereqs());
            // addedNewActions can be set from false to true, but once true it
            // should not be set back to false
            if (LOGGER.isDebugEnabled() && addedNewActions) {
                LOGGER.debug("Added prerequisites to requested action " 
                        + action.label());
            }    
        }
        
        return allActions;

    }

    /* Currently allowing only ntriples output format: Some processors MUST 
     * output ntriples, for two reasons: (1) append to file doesn't produce
     * valid RDF in RDFXML, since the <rdf> element is repeated (not sure 
     * whether json and ttl would work); (2) The links to blank nodes are lost,
     * since no identifier is assigned to them. (2) is no longer relevant once
     * BnodeConverter has applied. Later may consider commandline option to 
     * specify RDF output format, but for now simpler to only allow ntriple
     * output across the board. 
    private static RdfFormat getValidOutputFormat(String selectedFormat) {

        if (selectedFormat == null) {
            return DEFAULT_RDF_OUTPUT_FORMAT;
        } else {
            RdfFormat format = RdfFormat.get(selectedFormat);
            // *** TODO test that a bad format returns null here
            if (format != null) {
                return format;
            } else {
                LOGGER.error("Invalid format. Using default format "
                        + DEFAULT_RDF_OUTPUT_FORMAT.label() + ".");                   
                return DEFAULT_RDF_OUTPUT_FORMAT;               
            }
        }
    }
    */
    
    /**
     * Check for valid input directory. Return the absolute path to the input 
     * directory if it exists, otherwise log an error and return null. 
     * @param path - absolute or relative path to input directory
     * @return absolute path to the input directory if it exists, otherwise null
     */
    private static String getAbsoluteInputDir(String inDirName) {

        String absInputPath = null;
        
        File inDir = new File(inDirName);
        if (!inDir.isDirectory()) {
            LOGGER.fatal("Input directory " + inDirName
                    + " does not exist or is not a directory.");
        } else {
            try {
                absInputPath = inDir.getCanonicalPath();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        
        return absInputPath;
    }
    
    /**
     * Make output directory and any intermediate directories. Return the 
     * output directory if it was successfully created, otherwise log an error
     * and return null.
     * @param outDirName - absolute or relative path to output directory. A child
     * directory named with current datetime will be created under it.
     * @return the output directory if it was successfully created, otherwise 
     * null
     */
    private static String createTopLevelOutputDir(String outDirName) {
        
        String outDirCanonicalPath = null;
        
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd-HHmmss");
        Date date = new Date();
        String now = dateFormat.format(date);
        
        File outDir = new File(outDirName, now);
        
        try {
            outDirCanonicalPath = outDir.getCanonicalPath();
        } catch (IOException e) {
            e.printStackTrace();
            outDirCanonicalPath = outDir.getAbsolutePath();
        }
        
        if (! outDir.mkdirs()) {
            LOGGER.error("Cannot create output directory " 
                    + outDirCanonicalPath + ".");
            return null;
        }     
        
        return outDirCanonicalPath;
    }

        
    /**
     * Check for valid namespace within http scheme. Return the namespace if
     * valid, otherwise log an error and return null. 
     * @param namespace
     * @return valid namespace or null
     */
    private static boolean isValidNamespace(String namespace) {

        String[] schemes = {"http"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (!urlValidator.isValid(namespace)) {
            LOGGER.fatal("Valid HTTP namespace required.");
            return false;
        }
        
        return true;
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
            LOGGER.fatal(e.getMessage());
            printHelp(options);
        } catch (UnrecognizedOptionException e) {
            LOGGER.fatal(e.getMessage());
            printHelp(options);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            LOGGER.fatal(e.getStackTrace().toString());
        }
        return null;
    }
    
    
    /**
     * Define the commandline options accepted by the program.
     * @return an Options object
     */
    private static Options getOptions() {
        
        Options options = new Options();

        // For now the only defined action is "dedupe". Will add others later:
        // conversion to ld4l ontology, entity resolution, etc.
        Option actions = Option.builder("a")
                .longOpt("actions")
                .required()
                .hasArg()
                .desc("Processing actions. Valid actions: " 
                        + StringUtils.join(VALID_ACTIONS, ", ") + ".")
                .argName("dedupe")
                .build();
        options.addOption(actions);
        
//        Option formatOption = Option.builder("f")
//                .longOpt("format")
//                .required(false)
//                .hasArg()
//                .desc("RDF serialization format of input and output. Valid "
//                        + "formats: " 
//                        + StringUtils.join(VALID_RDF_OUTPUT_FORMATS, ", ")
//                        + ". Defaults to " 
//                        + DEFAULT_RDF_OUTPUT_FORMAT.label() + ".")
//                .build();
//        options.addOption(formatOption);  
        
        Option inputOption = Option.builder("i")
                .longOpt("indir")
                .required()
                .hasArg()
                .desc("Absolute or relative path to input files.")
                .build();
        options.addOption(inputOption);

        Option namespaceOption = Option.builder("n")
                .longOpt("namespace")
                .required()
                .hasArg()
                // Should namespace used in deduping?? Do we want to dedupe only 
                // uris in this namespace?
                .desc("Local HTTP namespace for minting and deduping URIs.")
                .build();
        options.addOption(namespaceOption);  
        
        Option outputOption = Option.builder("o")
                .longOpt("outdir")
                .required()
                .hasArg()
                .desc("Absolute or relative path to output directory.")
                .build();
        options.addOption(outputOption);
 
        return options;
    }
        
}


