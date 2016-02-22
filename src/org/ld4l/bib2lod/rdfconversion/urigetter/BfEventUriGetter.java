package org.ld4l.bib2lod.rdfconversion.urigetter;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfEventUriGetter extends ResourceUriGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfEventUriGetter.class);
    
    public BfEventUriGetter(Resource resource, String localNamespace) {
        super(resource, localNamespace);
    }

    @Override
    public String getUniqueKey() {
        // The only data we have for a bf:Event is bf:eventDate and 
        // bf:eventPlace. This isn't enough to reconcile resources; so 
        // generate only unique Event URIs.
        return resource.getLocalName();
    }
}
