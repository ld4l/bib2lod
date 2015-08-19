package org.ld4l.bib2lod;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
import org.ld4l.bib2lod.processor.ProcessController;


public class Bib2Lod {
    
    private static final Logger logger = LogManager.getLogger(Bib2Lod.class);
   
    private static final List<String> validActions = Action.validActions();
    
    private static final List<String> validFormats = RdfFormat.validFormats();
    private static final RdfFormat defaultFormat = RdfFormat.RDFXML;


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

        List<Action> actions = getValidActions(cmd.getOptionValues("actions"));
        if (actions == null) {
            return;
        }

        RdfFormat format = getValidFormat(cmd.getOptionValue("format"));

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
                format, absInputDir, absTopLevelOutputDir); 
        String absFinalOutpuDir = processController.processAll(actions);
        if (absFinalOutpuDir == null) {
            logger.trace("Processing failed.");
        } else {
            logger.trace("Done! Results in " + absFinalOutpuDir);
        }

    }

    private static RdfFormat getValidFormat(String selectedFormat) {

        if (selectedFormat == null) {
            return defaultFormat;
        } else {
            RdfFormat format = RdfFormat.get(selectedFormat);
            // *** TODO test that a bad format returns null here
            if (format != null) {
                return format;
            } else {
                logger.error("Invalid format. Using default format " 
                        + defaultFormat.format() + ".");                    
                return defaultFormat;                
            }
        }
    }

    /**
     * Validate actions.
     * Rudimentary validation - checks only for invalid actions. Doesn't check
     * for invalid combinations, etc.
     * @param actions
     * @return
     */
    private static List<Action> getValidActions(String[] selectedActions) {

        List<Action> actions = new ArrayList<Action>();
        for (String selectedAction : selectedActions) {
            Action action = Action.get(selectedAction);
            // *** TODO test that a bad action returns null here
            if (action != null) {
                actions.add(action);
            } else {
                logger.warn("Invalid action '" + selectedAction + "' removed.");
            }
            
        }
        
        return actions.isEmpty() ? null : actions;

    }


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
            logger.fatal("Input directory " + inDirName
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
            logger.error("Cannot create output directory " + outDirCanonicalPath + ".");
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
            logger.fatal("Valid HTTP namespace required.");
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
     * Define the commandline options accepted by the program.
     * @return an Options object
     */
    private static Options getOptions() {
        
        Options options = new Options();

        Option inputOption = Option.builder("i")
                .longOpt("indir")
                .required()
                .hasArg()
                .desc("Absolute or relative path to input files.")
                .build();
        options.addOption(inputOption);
        
        Option outputOption = Option.builder("o")
                .longOpt("outdir")
                .required()
                .hasArg()
                .desc("Absolute or relative path to output directory.")
                .build();
        options.addOption(outputOption);
        
        Option namespaceOption = Option.builder("n")
                .longOpt("namespace")
                .required()
                .hasArg()
                // Should namespace used in deduping?? Do we want to dedupe only 
                // uris in this namespace?
                .desc("Local HTTP namespace for minting and deduping URIs.")
                .build();
        options.addOption(namespaceOption);
        
        Option formatOption = Option.builder("f")
                .longOpt("format")
                .required(false)
                .hasArg()
                .desc("RDF serialization format of input and output. Valid"
                        + " formats: " + StringUtils.join(validFormats, ", ") 
                        + ". Defaults to " + defaultFormat.format() + ".")
                .build();
        options.addOption(formatOption);      

        // For now the only defined action is "dedupe". Will add others later:
        // conversion to ld4l ontology, entity resolution, etc.
        Option actions = Option.builder("a")
                .longOpt("actions")
                .required()
                .hasArg()
                .desc("Processing actions. Valid actions: " 
                        + StringUtils.join(validActions, ", ") + ".")
                .argName("dedupe")
                .build();
        options.addOption(actions);
        
        return options;
    }



        
}


