package org.ld4l.bib2lod.rdfconversion.reconciliation;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class AuthorityReconciler {

    private static final Logger LOGGER = 
            LogManager.getLogger(AuthorityReconciler.class);
    
    private final Resource resource;
    
    public AuthorityReconciler(Resource resource) {
        this.resource = resource;
    }
    
    public abstract String reconcile();

}
