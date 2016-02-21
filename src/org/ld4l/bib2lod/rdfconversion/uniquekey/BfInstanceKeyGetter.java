package org.ld4l.bib2lod.rdfconversion.uniquekey;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfInstanceKeyGetter extends ResourceKeyGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfInstanceKeyGetter.class);
    
    public BfInstanceKeyGetter(Resource resource) {
        super(resource);
    }

    @Override
    protected String getKey() {
        String key = null;
        if (key == null) {
            key = super.getKey();
        }
        return key;
    }
}
