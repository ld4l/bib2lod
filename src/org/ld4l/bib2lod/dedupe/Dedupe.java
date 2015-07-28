package org.ld4l.bib2lod.dedupe;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class Dedupe {
    
    /** 
     * Read in program arguments and call appropriate processing methods.
     * @param args
     */
    public static void main(String[] args) {

        // Define program arguments
        Options options = defineOptions();
        
        CommandLine cmd = getCommandLine(options, args);
        
        HashMap<String, String> cmdErrors = getCommandErrors(cmd);
        if (cmdErrors != null) {
            // log errors to logfile and stderr
            // exit
        }
        
        // Debug output. TODO use log4j instead.
//        Iterator<Option> i = cmd.iterator();
//        while (i.hasNext()) {
//            Option option = i.next();
//            String longopt = option.getLongOpt();
//            String opt = option.getOpt();
//            String value = option.getValue();
//            System.out.println(longopt + " " + opt + " " + value);
//            
//        }
        
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
    
    /** 
     * TODO Stub method to be implemented later. For now assume no errors.
     * @param cmd
     * @return
     */
    private static HashMap<String, String> getCommandErrors(CommandLine cmd) {
        // Maps option name to an error message
        HashMap<String, String> errors = new HashMap<String, String>();
        // Test for: 
        // all options supplied
        return errors;
    }

    private static CommandLine getCommandLine(Options options, String[] args) {
        
        // Parse program arguments
        CommandLineParser parser = new DefaultParser();
        
        try {
            return parser.parse(options, args);
        } catch (ParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Convert a directory of RDF files into a list of input streams for
     * processing.
     * @param directory - the directory
     * @return List<InputStream>
     */
    private static List<InputStream> readFiles(File directory) {
        // Convert the directory of RDF files into a list of input streams
        // for processing.

        // Despite the name, lists both files and directories.
        File[] items = directory.listFiles();
        List<InputStream> records = new ArrayList<InputStream>();
    
        for (File file : items) {
            if (file.isFile()) {
                try {
                    records.add(new FileInputStream(file));
                } catch (FileNotFoundException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        }  
        return records;
    }
    
    /**
     * Define the commandline arguments accepted by the programs.
     * @return
     */
    private static Options defineOptions() {
        
        Options options = new Options();
           
        // TODO This might change later: read from and write to triplestore;
        // read in and write out a graph?
        options.addOption("i", "inputdir", true, 
                "directory to read input files from");
        options.addOption("o", "outputdir", true, 
                "directory to write output to");
        
        // This is the namespace used by the LC converter in minting new URIs.
        options.addOption("n", "namespace", true, 
                "namespace of URIs to be deduped."); 
        
        return options;
    }
    
    /**
     * Convert a directory of RDF files into a list of input streams for
     * processing.
     * @param readdir - path to the directory
     * @return List<InputStream>
     */
    private static List<InputStream> readFiles(String readdir) {
        File directory = new File(readdir);
        return readFiles(directory);
    }
    
    private static String getOutputFilename(String outdir) {
        
        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd-HHmmss");
        Date date = new Date();
        String now = dateFormat.format(date);
        // Currently allowing only RDF/XML output. Later may offer 
        // serialization as a program argument. This will affect extension
        // as well as second parameter to allRecords.write(out, null) above.
        String ext = "rdf";
        String filename = "out." + now.toString() + "." + ext;
        Path path = Paths.get(outdir, filename);   
        return path.toString();
    }
}
