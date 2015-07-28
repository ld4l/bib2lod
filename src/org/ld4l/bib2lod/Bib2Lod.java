package org.ld4l.bib2lod;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.UnrecognizedOptionException;

public class Bib2Lod {

    /** 
     * Read in program options and call appropriate processing functionality.
     * @param args
     */
    public static void main(String[] args) {

        // Define program options
        Options options = defineOptions();
        
        // Get commandline options
        CommandLine cmd;
        try {
            cmd = getCommandLine(options, args);
        } catch (MissingOptionException e) {
            System.out.println(e.getMessage());
            printHelp(options);
            return;
        } catch (UnrecognizedOptionException e) {
            System.out.println(e.getMessage());
            printHelp(options);
            return;
        } catch (ParseException e) {
            e.printStackTrace();
            return;
        }

        
        // Debug output. TODO use log4j instead.
//        Iterator<Option> i = cmd.iterator();
//        while (i.hasNext()) {
//            Option option = i.next();
//            String longopt = option.getLongOpt();
//            String opt = option.getOpt();
//            String value = option.getValue();
//            System.out.println(longopt + " " + opt + " " + value);           
//        }
        
        createWorkspace(cmd.getOptionValue("inputdir"));
        
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
          System.out.println("This doesn't do anything except show that it\n"
                  + "runs. It contains a lot of code that is untested,\n"
                  + "unfinished, or left over from the demo thesis post-\n"
                  + "processor, largely for reference purposes. We will not\n"
                  + "use this class for now, but rather will call individual\n"
                  + "components of the post-processor as separate commandline\n"
                  + "processes.");
    }


    private static void printHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("bib2lod", options, true);
    }


    private static CommandLine getCommandLine(Options options, String[] args) throws ParseException {
        
        // Parse program arguments
        CommandLineParser parser = new DefaultParser();
        return parser.parse(options, args);
    }
    
    private static void createWorkspace(String string) {
        
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
     * Define the commandline arguments accepted by the programs.
     * @return
     */
    private static Options defineOptions() {
        Options options = new Options();
        
        // For now the only defined action is "dedupe". Will add others later:
        // conversion to ld4l ontology, entity resolution, etc.
        Option dedupeOption = Option.builder("d")
                .longOpt("dedupe")
                .required(false)
                .hasArg(false)
                .desc("dedupe URIs")
                .build();
        options.addOption(dedupeOption);
        
        Option inputOption = Option.builder("i")
                .longOpt("inputdir")
                .required()
                .hasArg()
                .desc("location of input files")
                .build();
        options.addOption(inputOption);
        
        Option namespaceOption = Option.builder("n")
                .longOpt("namespace")
                .required()
                .hasArg()
                .desc("namespace for minting and deduping URIs")
                .build();
        options.addOption(namespaceOption);
        
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


