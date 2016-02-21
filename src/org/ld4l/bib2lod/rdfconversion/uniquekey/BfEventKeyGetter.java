package org.ld4l.bib2lod.rdfconversion.uniquekey;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfEventKeyGetter extends ResourceKeyGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfEventKeyGetter.class);
    
    public BfEventKeyGetter(Resource resource) {
        super(resource);
    }

    @Override
    protected String getKey() {

        // The only data we have for a bf:Event is bf:eventDate and 
        // bf:eventPlace. This isn't enough to reconcile resources; so 
        // generate only unique Event URIs.
        return resource.getLocalName();
    }
}
