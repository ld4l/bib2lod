package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfTitleUriGenerator extends BfResourceUriGenerator {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTitleUriGenerator.class);
    
    public BfTitleUriGenerator(String localNamespace) {
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
