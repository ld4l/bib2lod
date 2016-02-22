package org.ld4l.bib2lod.rdfconversion.urigetter;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfClassificationUriGetter extends ResourceUriGetter {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(BfClassificationUriGetter.class);

    public BfClassificationUriGetter(Resource resource, String localNamespace) {
        super(resource, localNamespace);
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
