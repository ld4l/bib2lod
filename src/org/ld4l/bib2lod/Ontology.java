package org.ld4l.bib2lod;

public enum Ontology {

    // TODO Would be better not to have to hard-code these, especially the 
    // filenames. Figure out a better approach. See notes in ProcessController.
    // We do need a reference to some of the ontology models, so an array
    // or list of models won't work.
    BIBFRAME ("http://bibframe.org/vocab/", "bf", "bibframe.2014-12-10.rdf"),
    LD4L ("http://bibframe.ld4l.org/ontology/", "ld4l", "bibframe.ld4l.rdf"),
    DCTERMS ("http://purl.org/dc/terms/", "dc"),
    FOAF ("http://http://xmlns.com/foaf/0.1/", "foaf"),
    MADSRDF ("http://www.loc.gov/mads/rdf/v1#", "madsrdf"), // check if needed
    RELATORS ("http://id.loc.gov/vocabulary/relators/", "marcrel"); // check if needed
    
    private final String namespace;
    private final String prefix;
    private final String filename;
    
    // Ontologies that are loaded from a file in the application
    Ontology(String namespace, String prefix, String filename) {
        this.namespace = namespace;
        this.prefix = prefix;
        this.filename = filename;
    }
    
    // Ontologies that are referenced but not loaded, or loaded from their 
    // URIs.
    Ontology(String namespace, String prefix) {
        this.namespace = namespace;
        this.prefix = prefix;
        this.filename = null;
    }
    
    public String namespace() {
        return this.namespace;
    }
    
    public String prefix() {
        return this.prefix;
    }
    
    public String filename() {
        return this.filename;
    }
}
