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
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Bib2Lod {
    
    private static final Logger LOGGER = LogManager.getLogger(Bib2Lod.class);  
    private static final List<String> VALID_ACTIONS = Action.validActions();
    
    /** 
     * Read in program options and call appropriate processing functionality.
     * @param args
     */
    public static void main(String[] args) {

        LOGGER.info("START CONVERSION.");
        
        // Define program options
        Options options = getOptions();
        
        // Get commandline options
        CommandLine cmd = getCommandLine(options, args);
        if (cmd == null) {
            return;
        }
        
        boolean erase = ! cmd.hasOption("no_erase");
        
        boolean addPrereqs = ! cmd.hasOption("no_prereqs");

        // Process commandline arguments and exit if any are invalid.
        String namespace = cmd.getOptionValue("namespace");
        if (!isValidNamespace(namespace)) {
            return;
        }
     
        Set<Action> actions = getValidActions(cmd.getOptionValues("action"), 
                addPrereqs);
        if (actions == null) {
            LOGGER.debug("No valid actions specified. Exiting.");
            return;
        }

        String absInputDir = getAbsoluteInputDir(cmd.getOptionValue("indir"));                
        if (absInputDir == null) {
            return;
        }

        String absTopLevelOutputDir = 
                createTopLevelOutputDir(cmd.getOptionValue("outdir"));
        if (absTopLevelOutputDir == null) {
            return;
        }
           
        // Log application configuration settings
        LOGGER.info("Settings: ");
        LOGGER.info("Local namespace: " + namespace);
        LOGGER.info("Input directory: " + absInputDir);
        LOGGER.info("Output directory: " + absTopLevelOutputDir);
        LOGGER.info("Add prerequisite actions: " + addPrereqs);               
        if (LOGGER.isInfoEnabled()) {
            List<String> actionLabels = new ArrayList<String>();
            for (Action a : actions) {
                actionLabels.add(a.label());
            }
            String info = addPrereqs ? "Actions to be executed, including "
                    + "prerequisites: " : "Actions to be executed: ";
            LOGGER.info(info + actionLabels.toString());
        }
        LOGGER.info("Delete intermediate output directories: " + erase);


        
        ProcessController processController = new ProcessController(namespace, 
                absInputDir, absTopLevelOutputDir, erase, addPrereqs); 
        String absFinalOutputDir = processController.processAll(actions);
        if (absFinalOutputDir == null) {
            LOGGER.error("Processing failed.");
        } 
    }


    /**
     * Validate actions.
     * Removes invalid actions and adds missing prerequisites.
     * @param actions
     * @return
     */
    private static Set<Action> getValidActions(String[] selectedActions,
            boolean addPrereqs) {

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
        if (addPrereqs) {
            EnumSet<Action> allActions = actions.clone();
            for (Action action : actions) {
                boolean addedNewActions = 
                        allActions.addAll(action.recursivePrereqs());
                if (LOGGER.isTraceEnabled() && addedNewActions) {
                    LOGGER.trace("Added prerequisites to requested action " 
                            + action.label());
                }   
            }
            return allActions;
            
        } else {
            return actions;
        }
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
        formatter.printHelp("java -jar Bib2Lod.jar", options, true);
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

        options.addOption(Option.builder("a")
                .longOpt("action")
                .required()
                .hasArg()
                .desc("Processing action. Valid actions: " 
                        + StringUtils.join(VALID_ACTIONS, ", ") + ". Can be "
                        + "invoked more than once to specify multiple actions.")
                .argName("action")
                .build());
        

//        options.addOption(Option.builder("f")
//                .longOpt("format")
//                .required(false)
//                .hasArg()
//                .desc("Serialization format of input and output. Valid "
//                        + "formats: " 
//                        + Bib2LodStringUtils.join(VALID_OUTPUT_FORMATS, ", ")
//                        + ". Defaults to " 
//                        + DEFAULT_OUTPUT_FORMAT.label() + ".")
//                .build());
        
        options.addOption(Option.builder("i")
                .longOpt("indir")
                .required()
                .hasArg()
                .argName("input_directory")
                .desc("Absolute or relative path to directory containing " 
                        + "input files.")
                .build());

        options.addOption(Option.builder("n")
                .longOpt("namespace")
                .required()
                .hasArg()
                .argName("local_namespace")
                .desc("Local HTTP namespace for minting and deduping URIs.")
                .build()); 

        options.addOption(Option.builder("ne")
                .longOpt("no_erase")
                .required(false)
                .hasArg(false)
                .desc("Keep intermediate output. Default is to erase.")
                .build());
        
        options.addOption(Option.builder("np")
                .longOpt("no_prereqs")
                .required(false)
                .hasArg(false)
                .desc("Do not add prerequisite actions to those specified. " 
                        + "Allows restarting at intermediate processing "
                        + "stages. Default is to add prereqs. This option "
                        + "should be used ONLY when intermediate output is "
                        + "used as input; it cannot be used to sidestep "
                        + "processing steps altogether, since some later steps "
                        + "assume the application of earlier ones. All "
                        + "subsequent steps must be specified on the "
                        + "commandline.")
                .build());
        
        options.addOption(Option.builder("o")
                .longOpt("outdir")
                .required()
                .hasArg()
                .argName("output_directory")
                .desc("Absolute or relative path to output directory. "
                        + "Will be created if it does not exist.")
                .build());
 
        return options;
    }
        
}


