package org.ld4l.bib2lod;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
         * Test whether reading in and/or writing out a Jena model removes 
         * duplicate triples.
         * Result: duplicates removed when reading the file into a model.
         */
        String infile = "testinput/jenatest/test.nt";
        String outfile = "output/jenatest/test.nt";
        try {
            long incount = Files.newBufferedReader(Paths.get(infile)).lines().count();
            LOGGER.debug("Input file size: " + incount);
            Model model = ModelFactory.createDefaultModel() ; 
            model.read(infile);
            LOGGER.debug("Triples in model: " + model.size());
            FileOutputStream outStream;
            outStream = new FileOutputStream(outfile);
            RDFDataMgr.write(outStream, model, RDFFormat.NTRIPLES);
            long outcount = Files.newBufferedReader(Paths.get(outfile)).lines().count();
            LOGGER.debug("Input file size: " + outcount);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        LOGGER.info("Done!");

    }

}
