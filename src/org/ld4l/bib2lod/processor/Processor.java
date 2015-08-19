package org.ld4l.bib2lod.processor;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.RdfFormat;


public abstract class Processor {

    @SuppressWarnings("unused")
    private static final Logger logger = LogManager.getLogger(Processor.class);
    
    protected String localNamespace;
    protected RdfFormat rdfFormat;
    
    protected String inputDir;
    private String mainOutputDir;
    
    
    public Processor(String localNamespace, RdfFormat rdfFormat, String inputDir, 
            String mainOutputDir) {
        
        this.localNamespace = localNamespace;
        this.rdfFormat = rdfFormat;
        
        this.inputDir = inputDir;
        this.mainOutputDir = mainOutputDir;

    }
    
    public abstract String process();
    
    public Model getModelFromFile(File file) {
        return getModelFromFile(file.toString());
    }
    
    public Model getModelFromFile(String filename) {
        return RDFDataMgr.loadModel(filename);
    }
    
    /**
     * Create a subdirectory of main output directory for the output of this
     * process.
     * @param subDirName - the output subdirectory name for this process
     * @return - the full output directory path
     */
    protected String createOutputDir(String subdir) {
        
        String outputDir = null;
        
        try {
            outputDir = Files.createDirectory(Paths.get(mainOutputDir, 
                    subdir)).toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return outputDir;
    }

//    Following probably not needed - Jena can read from file directly into model
//    /**
//     * Convert a directory of RDF files into a list of input streams for
//     * processing.
//     * @param directory - the directory
//     * @return List<InputStream>
//     */
//    protected List<InputStream> readRdfFiles(File directory) {
//        // Convert the directory of RDF files into a list of input streams
//        // for processing.
//
//        // Despite the name, lists both files and directories.
//        File[] items = directory.listFiles();
//        List<InputStream> inStreams = new ArrayList<InputStream>();
//    
//        for (File file : items) {
//            if (file.isFile()) {
//                try {
//                    inStreams.add(new FileInputStream(file));
//                } catch (FileNotFoundException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//            }
//        }  
//        return inStreams;
//    }
    
//    /**
//     * Convert a directory of RDF files into a list of input streams for
//     * processing.
//     * @param readdir - path to the directory
//     * @return List<InputStream>
//     */
//    protected List<InputStream> readRdfFiles(String readdir) {
//        File directory = new File(readdir);
//        return readRdfFiles(directory);
//    }
 
}
