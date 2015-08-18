package org.ld4l.bib2lod;

public enum Ontology {

    BIBFRAME ("http://bibframe.org/vocab/"),
    BIBFRAME_LD4L ("http://bibframe.ld4l.org/ontology/"),
    DCTERMS ("http://purl.org/dc/terms/"),
    FOAF ("http://http://xmlns.com/foaf/0.1/"),
    MADSRDF ("http://www.loc.gov/mads/rdf/v1#"), // check if needed
    RELATORS ("http://id.loc.gov/vocabulary/relators/"); // check if needed
    
    private final String namespace;
    
    Ontology(String namespace) {
        this.namespace = namespace;
    }
    
    public String namespace() {
        return this.namespace;
    }
}
