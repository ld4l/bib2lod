package org.ld4l.bib2lod.rdfconversion.reconciliation;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TopicAuthorityReconciler extends AuthorityReconciler {

    private static final Logger LOGGER = 
            LogManager.getLogger(TopicAuthorityReconciler.class);
    
    public TopicAuthorityReconciler(Resource resource) {
        super(resource);
    }

    @Override
    public String reconcile() {
        String uri = null;

        return uri;
    }
    
    

}
