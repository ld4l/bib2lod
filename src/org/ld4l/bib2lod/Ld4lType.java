package org.ld4l.bib2lod;

public enum Ld4lType {

    PERSON ("Person"),
    WORK ("Work"),
    INSTANCE ("Instance"),
    ORGANIZATION (Ontology.FOAF.namespace(), "Organization"),
    SUBJECT (Ontology.DCTERMS.namespace(), "Subject");

    private final String namespace;
    private final String localname;
    // private final String uri;

    /** 
     * Constructor for types in external namespaces.
     * 
     * @param namespace
     * @param localname
     */    
    Ld4lType(String namespace, String localname) {
        this.namespace = namespace;
        this.localname = localname;
        // this.uri = namespace + localname;
    }

    /**
     * Constructor for types in LD4L namespace.
     * @param localname
     */
    Ld4lType(String localname) {
        this(Ontology.LD4L.namespace(), localname);
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
