package org.ld4l.bib2lod.rdfconversion.uniquekey;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO Will need to treat topics differently - URI should come from schemes
// like FAST. Will not just need to send back a key.
public class BfTopicKeyGetter extends ResourceKeyGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTopicKeyGetter.class);
    
    public BfTopicKeyGetter(Resource resource) {
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
