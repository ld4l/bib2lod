package org.ld4l.bib2lod.rdfconversion;

import static java.nio.file.StandardCopyOption.COPY_ATTRIBUTES;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public abstract class RdfProcessor {

    private static final Logger LOGGER = LogManager.getLogger(RdfProcessor.class);   
    
    /* Some processors MUST output ntriples, for two reasons: (1) append to 
     * file doesn't produce valid RDF in RDFXML, since the <rdf> element is
     * repeated (not sure whether json and ttl would work); (2) The links to 
     * blank nodes are lost, since no identifier is assigned to them. (2) is no
     * longer relevant once BnodeConverter has applied.  
       private static final RdfFormat DEFAULT_RDF_OUTPUT_FORMAT = 
            RdfFormat.NTRIPLES;
     */
    private static final RdfFormat RDF_OUTPUT_FORMAT = RdfFormat.NTRIPLES;
            
    private final String outputSubdir;
    protected final String localNamespace;    
    protected final String inputDir;
    private final String mainOutputDir;
    protected OntModel bfOntModelInf; 

       
    public RdfProcessor(OntModel bfOntModelInf, String localNamespace,  
            String inputDir, String mainOutputDir) {

        this(localNamespace, inputDir, mainOutputDir);      
        this.bfOntModelInf = bfOntModelInf;  
    }
    
    /**
     * Constructor for processes that don't use the loaded OntModel(s).
     * @param localNamespace
     * @param inputDir
     * @param mainOutputDir
     */
    public RdfProcessor(String localNamespace, String inputDir, 
            String mainOutputDir) {

        LOGGER.trace("In constructor for " + this.getClass().toString());        
        this.localNamespace = localNamespace;       
        this.inputDir = inputDir;
        this.mainOutputDir = mainOutputDir;
        this.outputSubdir = 
                StringUtils.substringAfterLast(this.getClass().getName(), "."); 
               
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
    
    protected RdfFormat getRdfOutputFormat() {
        /* Currently allowing only ntriples output format: Some processors MUST 
         * output ntriples, for two reasons: (1) append to file doesn't produce
         * valid RDF in RDFXML, since the <rdf> element is repeated (not sure 
         * whether json and ttl would work); (2) The links to blank nodes are 
         * lost, since no identifier is assigned to them. (2) is no longer 
         * relevant once BnodeConverter has applied. Later may consider 
         * commandline option to specify RDF output format, but for now simpler 
         * to only allow ntriple output across the board. If multiple rdf output 
         * formats are  allowed, the output format for a particular processor 
         * should be  determined as follows, in order of precedence:
         * 1. Individual Processor requirement
         * 2. Commandline option
         * 3. Same format as input
         */
        return RDF_OUTPUT_FORMAT;
    }
    
    /**
     * Create a subdirectory of main output directory for the output of this
     * process.
     * @return - the full output directory path
     */
    protected String createOutputDir() {
        
        String outputDir = null;
        try {
            outputDir = Files.createDirectory(Paths.get(mainOutputDir, 
                    outputSubdir)).toString();
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
        writeModelToFile(model, file, true);
    }

    protected void writeModelToFile(Model model, File file) {
        writeModelToFile(model, file, false);
    }
    
    protected void writeModelToFile(Model model, File file, boolean append) {
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(file, append);
            RDFDataMgr.write(outStream, model, 
                    getRdfOutputFormat().jenaRDFFormat());
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
    }
    
    protected String getOutputFilename(String basename) {
        //return basename + "." +  getRdfOutputFormat().extension(); 
        return basename + getRdfOutputFormat().fullExtension();
    }
    
    protected String stubProcess() {
        String outputDir = createOutputDir();
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
 
}
