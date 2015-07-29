package org.ld4l.bib2lod;

import java.io.File;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;
import org.apache.commons.validator.routines.UrlValidator;


public class Bib2Lod {

    /** 
     * Read in program options and call appropriate processing functionality.
     * @param args
     */
    public static void main(String[] args) {

        // Define program options
        Options options = defineOptions();
        
        // Get commandline options
        CommandLine cmd = null;
        try {
            cmd = getCommandLine(options, args);
        } catch (MissingOptionException e) {
            System.out.println(e.getMessage());
            printHelp(options);
            System.exit(0);
        } catch (UnrecognizedOptionException e) {
            System.out.println(e.getMessage());
            printHelp(options);
            System.exit(0);
        } catch (ParseException e) {
            e.printStackTrace();
            System.exit(0);
        }
        
        File outDir = new File(cmd.getOptionValue("outdir"));
        if (! outDir.mkdirs()) {
            if (outDir.isDirectory()) {
                System.out.println("Error: output directory exists.");
            } else {
                System.out.println("Error: cannot create output directory.");
            }
            System.exit(0);
        }
        
        File inDir = new File(cmd.getOptionValue("indir"));
        if (!inDir.isDirectory()) {
            System.out.println("Error: input directory does not exist or is "
                    + "not a directory.");
            System.exit(0);
        }
        
        String namespace = cmd.getOptionValue("namespace");
        String[] schemes = {"http"};
        UrlValidator urlValidator = new UrlValidator(schemes);
        if (!urlValidator.isValid(namespace)) {
            System.out.println("Error: Valid HTTP namespace required.");
            System.exit(0);
        }
        
        // TODO - make sure there's at least one action - but leave complexity
        // for later - if two actions defined, must do those in between also
        // - can't skip actions in the middle - for now just run one at a time
        
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
          System.out.println("Done!");
    }


    /**
     * Print help text.
     * @param options
     */
    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("bib2lod", options, true);
    }


    /**
     * Parse commandline options.
     * @param options
     * @param args
     * @return
     * @throws ParseException
     */
    private static CommandLine getCommandLine(Options options, String[] args) 
            throws ParseException {
        
        // Parse program arguments
        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
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
     * @return
     */
    private static Options defineOptions() {
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
                .desc("http namespace for minting and deduping URIs")
                .build();
        options.addOption(namespaceOption);

        // For now the only defined action is "dedupe". Will add others later:
        // conversion to ld4l ontology, entity resolution, etc.
        Option dedupeAction = Option.builder("d")
                .longOpt("dedupe")
                .required(false)
                .hasArg(false)
                .desc("dedupe")
                .build();
        options.addOption(dedupeAction);
        
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


