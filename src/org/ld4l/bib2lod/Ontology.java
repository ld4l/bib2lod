package org.ld4l.bib2lod;

public enum Ontology {

    // TODO It's not desirable to hard-code the filenames. Figure out another
    // approach.
    BIBFRAME ("http://bibframe.org/vocab/", "bibframe.2014-12-10.rdf"),
    LD4L ("http://bibframe.ld4l.org/ontology/", "bibframe.ld4l.rdf"),
    DCTERMS ("http://purl.org/dc/terms/"),
    FOAF ("http://http://xmlns.com/foaf/0.1/"),
    MADSRDF ("http://www.loc.gov/mads/rdf/v1#"), // check if needed
    RELATORS ("http://id.loc.gov/vocabulary/relators/"); // check if needed
    
    private final String namespace;
    private final String filename;
    
    // Ontologies that are loaded from a file in the application
    Ontology(String namespace, String filename) {
        this.namespace = namespace;
        this.filename = filename;
    }
    
    // Ontologies that are referenced but not loaded, or loaded from their 
    // URIs.
    Ontology(String namespace) {
        this.namespace = namespace;
        this.filename = null;
    }
    
    public String namespace() {
        return this.namespace;
    }
    
    public String filename() {
        return this.filename;
    }
}
