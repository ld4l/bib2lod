package org.ld4l.bib2lod;

public enum Ld4lType {

    PERSON (Ontology.BIBFRAME_LD4L.namespace(), "Person"),
    WORK (Ontology.BIBFRAME_LD4L.namespace(), "Work"),
    INSTANCE (Ontology.BIBFRAME_LD4L.namespace(), "Instance"),
    ORGANIZATION (Ontology.FOAF.namespace(), "Organization"),
    SUBJECT (Ontology.DCTERMS.namespace(), "Subject");

    private final String namespace;
    private final String localname;
    // private final String uri;

    Ld4lType(String namespace, String localname) {
        this.namespace = namespace;
        this.localname = localname;
        // this.uri = namespace + localname;
    }
    
    public String uri() {
        // return this.uri;
        return this.namespace + this.localname;
    }
    
    public String namespace() {
        return this.namespace();
    }
    
    public String localname() {
        return this.localname;
    }
}