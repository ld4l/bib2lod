package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfAnnotationUriGenerator extends BfResourceUriGenerator {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfAnnotationUriGenerator.class);
    
    public BfAnnotationUriGenerator(String localNamespace) {
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
