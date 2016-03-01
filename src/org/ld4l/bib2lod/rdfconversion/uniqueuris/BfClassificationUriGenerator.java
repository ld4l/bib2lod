package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfClassificationUriGenerator extends BfResourceUriGenerator {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(BfClassificationUriGenerator.class);

    public BfClassificationUriGenerator(String localNamespace) {
        super(localNamespace);
    }
    
    @Override
    public String getUniqueKey() {
        
        String key = null;
        if (key == null) {
            key = super.getUniqueKey();
        }
        return key;
    }

}
