package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfType;

public class BfIdentifierConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfIdentifierConverter.class);
    
    // TODO - which constructors are needed?
    public BfIdentifierConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
        // TODO Auto-generated constructor stub
    }


    public BfIdentifierConverter(String localNamespace, Statement statement) {
        super(localNamespace, statement);
        // TODO Auto-generated constructor stub
    }
    

}
