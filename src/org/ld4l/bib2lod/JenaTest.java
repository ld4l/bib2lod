package org.ld4l.bib2lod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.RDFFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class JenaTest {
    
    private static final Logger LOGGER = LogManager.getLogger(JenaTest.class);

    public static void main(String[] args) {
        
        /**
         * Test whether reading into a Jena model removes duplicate triples.
         * Result: yes.
         */
        String infile = "testinput/jenatest/test.nt";
        Model model = ModelFactory.createDefaultModel() ; 
        model.read(infile);
        
        File outfile = new File("output/jenatest/test.nt");
        FileOutputStream outStream;
        try {
            outStream = new FileOutputStream(outfile);
            RDFDataMgr.write(outStream, model, RDFFormat.NTRIPLES);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }   
        
        LOGGER.info("Done!");

    }

}
