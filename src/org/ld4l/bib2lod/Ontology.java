package org.ld4l.bib2lod;

public enum Ontology {

    BIBFRAME_LD4L ("http://bibframe.ld4l.org/ontology/"),
    DCTERMS ("http://purl.org/dc/terms/"),
    FOAF ("http://http://xmlns.com/foaf/0.1/");
    
    private final String namespace;
    
    Ontology(String namespace) {
        this.namespace = namespace;
    }
    
    public String namespace() {
        return this.namespace;
    }
}
