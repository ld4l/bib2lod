package org.ld4l.bib2lod.rdfconversion.urigetter;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

// TODO Will need to treat topics differently - URI should come from schemes
// like FAST. Will not just need to send back a key.
public class BfTopicUriGetter extends ResourceUriGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTopicUriGetter.class);
    
    public BfTopicUriGetter(Resource resource, String localNamespace) {
        super(resource, localNamespace);
    }

    @Override
    public String getUniqueUri() {
        String key = null;
        if (key == null) {
            key = super.getUniqueUri();
        }
        return key;
    }
    
}
