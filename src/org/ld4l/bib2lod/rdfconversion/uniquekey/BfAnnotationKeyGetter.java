package org.ld4l.bib2lod.rdfconversion.uniquekey;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfAnnotationKeyGetter extends ResourceKeyGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfAnnotationKeyGetter.class);
    
    public BfAnnotationKeyGetter(Resource resource) {
        super(resource);
    }

}
