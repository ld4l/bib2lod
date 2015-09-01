package org.ld4l.bib2lod;

public enum Namespace {

    // TODO Would be better not to have to hard-code these, especially the 
    // filenames. Figure out a better approach. See notes in ProcessController.
    // We do need a reference to some of the ontology models, so an array
    // or list of models won't work.
    BIBFRAME ("http://bibframe.org/vocab/"),
    LD4L ("http://bibframe.ld4l.org/ontology/"),
    DCTERMS ("http://purl.org/dc/terms/"),
    FOAF ("http://http://xmlns.com/foaf/0.1/"),
    MADSRDF ("http://www.loc.gov/mads/rdf/v1#"), // check if needed
    RELATORS ("http://id.loc.gov/vocabulary/relators/"); // check if needed
    
    private final String namespace;
    
    // Ontologies that are loaded from a file in the application
    Namespace(String namespace) {
        this.namespace = namespace;
    }
    
    public String namespace() {
        return this.namespace;
    }
    
}
