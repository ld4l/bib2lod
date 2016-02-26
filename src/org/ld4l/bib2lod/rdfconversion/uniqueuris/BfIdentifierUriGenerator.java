package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfIdentifierUriGenerator extends ResourceUriGenerator {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfIdentifierUriGenerator.class);
    
    public BfIdentifierUriGenerator(Resource resource, String localNamespace) {
        super(resource, localNamespace);
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
