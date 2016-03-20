package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class BfIdentifierUriGenerator extends BfResourceUriGenerator {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfIdentifierUriGenerator.class);
    
    public BfIdentifierUriGenerator(String localNamespace) {
        super(localNamespace);
    }

    @Override
    protected String getUniqueKey() {
        
        String key = null;
        if (key == null) {
            key = super.getUniqueKey();
        }
        return key;
    }
    
}
