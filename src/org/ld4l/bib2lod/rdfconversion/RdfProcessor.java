package org.ld4l.bib2lod.rdfconversion;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.ontology.OntModel;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RiotException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.Format;
import org.ld4l.bib2lod.Processor;


public abstract class RdfProcessor extends Processor {

    private static final Logger LOGGER = 
            LogManager.getLogger(RdfProcessor.class);   
    
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
    protected static final Format RDF_OUTPUT_FORMAT = Format.NTRIPLES;
            
    protected final String localNamespace;    
    // protected OntModel bfOntModel; 

       
//    public RdfProcessor(OntModel bfOntModel, String localNamespace,  
//            String inputDir, String mainOutputDir) {
//
//        this(localNamespace, inputDir, mainOutputDir);      
//        this.bfOntModel = bfOntModel;  
//    }
    
    /**
     * Constructor for processors that don't use the loaded OntModel(s).
     * @param localNamespace
     * @param inputDir
     * @param mainOutputDir
     */
    public RdfProcessor(String localNamespace, String inputDir, 
            String mainOutputDir) {
        super(inputDir, mainOutputDir);
        LOGGER.trace("In constructor for " + this.getClass().toString());        
        this.localNamespace = localNamespace;                      
    }
    
    /** 
     * Constructor for processors that don't use the loaded OntModel(s) or the
     * local namespace.
     * @param inputDir
     * @param mainOutputDir
     */
    public RdfProcessor(String inputDir, String mainOutputDir) {
        super(inputDir, mainOutputDir);
        this.localNamespace = null;
    }
    
    public abstract String process();
     
    protected Model readModelFromFile(File file) {
        return readModelFromFile(file.toString());
    }
    
    protected Model readModelFromFile(String filename) {
        //return RDFDataMgr.loadModel(filename);
        // LOGGER.debug("Reading file " + filename);
        Model model = ModelFactory.createDefaultModel(); 
        try {
            model.read(filename);
        } catch (RiotException e) {
           LOGGER.error("ERROR: RDF parsing error in file " 
                   + FilenameUtils.getName(filename) + ": " + e.getMessage()
                   + ". Skipping rest of file.");
        }
        return model;
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
    
    protected Format getOutputFormat() {
        return RDF_OUTPUT_FORMAT;
    }
    
    protected void appendModelToFile(Model model, File file) {
        writeModelToFile(model, file, true);
    }

    protected void appendModelToFile(Model model, String basename) {
        writeModelToFile(model, basename, true);
    }
    
    protected void writeModelToFile(Model model, File file) {
        writeModelToFile(model, file, false);
    }
    
    protected void writeModelToFile(Model model, String basename) {
        writeModelToFile(model, basename, false);
    }
    
    private void writeModelToFile(Model model, String basename, boolean append) {
        File outputFile = new File(getOutputDir(), getOutputFilename(basename));
        writeModelToFile(model, outputFile, append);
    }
    
    private void writeModelToFile(Model model, File file, boolean append) {
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(file, append);
            RDFDataMgr.write(outStream, model, 
                    getOutputFormat().jenaRDFFormat());
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }        
    }

    // For development/debugging
    public static void printModel(Model model, Level level, String msg) {
        
        if (msg != null) {
            LOGGER.log(level, msg);
        }
        int stmtCount = 0;
        StmtIterator statements = model.listStatements();   
        while (statements.hasNext()) {
            stmtCount++;
            Statement statement = statements.nextStatement();
            // NB The level must be enabled for this class,  not the calling
            // class.
            // TODO Programmatically set LOGGER level to level, then set back
            // to previous level. Complicated because log4j2 doesn't have a
            // Logger.setLevel() method like log4j. 
            LOGGER.log(level, stmtCount + ". " + statement.toString());
        }     
    }
    
    public static void printModel(Model model, Level level) {
        printModel(model, level, null);
    }
    
    public static void printModel(Model model) {
        printModel(model, Level.DEBUG, null);
    }
    
    public static void printModel(Model model, String msg) {
        printModel(model, Level.DEBUG, msg);
    }
    
    public static String mintUri(String namespace) {    
        return namespace + mintLocalName();
    }
    
    public static String mintLocalName() {
        // NB A digit is not a legal initial character of a local name in 
        // RDF/XML; see http://www.w3.org/TR/xml11/#NT-NameStartChar, so 
        // prefix a character to the UUID.
        return "n" + UUID.randomUUID().toString();
    }

}
