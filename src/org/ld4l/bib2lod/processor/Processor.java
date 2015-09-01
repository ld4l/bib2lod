package org.ld4l.bib2lod.processor;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class Processor {

    private static final Logger logger = LogManager.getLogger(Processor.class);   
    
    private static final RDFFormat DEFAULT_FORMAT = RDFFormat.NTRIPLES;
    private static final String DEFAULT_FORMAT_FILE_EXTENSION = "nt";

    protected OntModel bfOntModelInf;
    
    protected String localNamespace;
    
    protected String inputDir;
    private String mainOutputDir;

       
    public Processor(OntModel bfOntModelInf, String localNamespace,  
            String inputDir, String mainOutputDir) {

        // logger.trace("In constructor for " + this.getClass().toString());
        
        this.bfOntModelInf = bfOntModelInf;
        
        this.localNamespace = localNamespace;
        
        this.inputDir = inputDir;
        this.mainOutputDir = mainOutputDir;

    }
    
    /**
     * Constructor for processes that don't use OntModels.
     * @param localNamespace
     * @param inputDir
     * @param mainOutputDir
     */
    public Processor(String localNamespace, String inputDir, 
            String mainOutputDir) {

        // logger.trace("In constructor for " + this.getClass().toString());        
        this.localNamespace = localNamespace;       
        this.inputDir = inputDir;
        this.mainOutputDir = mainOutputDir;
    }
    
    public abstract String process();
    
    protected Model readModelFromFile(File file) {
        return readModelFromFile(file.toString());
    }
    
    protected Model readModelFromFile(String filename) {
        //return RDFDataMgr.loadModel(filename);
        Model model = ModelFactory.createDefaultModel() ; 
        model.read(filename);
        return model;
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
    
    /** 
     * Debugging method: Output all statements in a model
     */
    protected String showStatements(Model model) {
        
        StringBuffer buffer = new StringBuffer();
        
        StmtIterator stmts = model.listStatements();
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            Resource subject = stmt.getSubject();
            String subjectValue = null;
            if (subject.isURIResource()) {
                subjectValue = "<" + subject.getURI() + ">";
            } else {
                subjectValue = subject.toString();
            }
            String propertyUri = "<" + stmt.getPredicate().getURI() + ">";
            RDFNode object = stmt.getObject();
            String objectValue = null;
            if (object.isLiteral()) {
                objectValue = "\"" + object.asLiteral().getLexicalForm() + "\"";
            } else if (object.isURIResource()) {
                objectValue = "<" + object.asResource().getURI() + ">";
            } else {
                objectValue = object.toString();
            }
            buffer.append("Statement: " + subjectValue + " " + propertyUri + 
                    " " + objectValue + "\n");
        }
        return buffer.toString();
    }
    
    protected void appendModelToFile(Model model, File file) {
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(file, true);
            RDFDataMgr.write(outStream, model, DEFAULT_FORMAT);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    protected String getOutputFilename(String basename) {
        return basename + "." +  DEFAULT_FORMAT_FILE_EXTENSION;   
    }
    
    protected String stubProcess(String outputSubDir) {
        String outputDir = createOutputDir(outputSubDir);
        copyFiles(inputDir, outputDir);
        return outputDir;
    }
    
    protected void copyFiles(String inputDir, String outputDir) { 
        
        for ( File file : new File(inputDir).listFiles() ) {
            Path source = file.toPath();
            String sourceFileName = file.getName();
            Path target = new File(outputDir, sourceFileName).toPath();
            try {
                Files.copy(source, target, COPY_ATTRIBUTES);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
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
