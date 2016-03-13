package org.ld4l.bib2lod.rdfconversion;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum Ld4lIndividual {

    MOTIVATION_REVIEWING("reviewing"),
    MOTIVATION_SUMMARIZING("summarizing"),
    
    SOURCE_STATUS_SUPPLIED("supplied"),
    SOURCE_STATUS_TRANSCRIBED("transcribed");

    
    private static final Logger LOGGER = 
            LogManager.getLogger(Ld4lIndividual.class);
            
    
    private final OntNamespace namespace;
    private final String localname;
    private final String uri;
    private final Resource individual;
    
    Ld4lIndividual(String localname) {
        // Default namespace for this enum type.
        this(OntNamespace.LD4L, localname);
    }
    
    Ld4lIndividual(OntNamespace namespace, String localname) {
        this.namespace = namespace;
        this.localname = localname;
        this.uri = namespace.uri() + localname;
        this.individual = ResourceFactory.createResource(uri);
    }
    
    public String localname() {
        return localname;
    }
    
    public OntNamespace namespace() {
        return namespace;
    }
    
    public String namespaceUri() {
        return namespace.uri();
    }
    
    public Resource individual() {
        return individual;
    }
}
