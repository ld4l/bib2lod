package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

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
