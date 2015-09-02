package org.ld4l.bib2lod;

public enum OntologyProperty {

    HAS_AUTHORITY (Namespace.BIBFRAME.uri(), "hasAuthority"),
    IDENTIFIER (Namespace.BIBFRAME.uri(), "identifier");
    
    private final String namespace;
    private final String localname;
    private final String uri;
    
    OntologyProperty(String namespace, String localname) {
        // Or should this be a Namespace?
        this.namespace = namespace;
        this.localname = localname;
        this.uri = namespace + localname;
    }
    
    public String namespace() {
        return this.namespace;
    }
    
    public String localname() {
        return this.localname;
    }
    
    public String uri() {
        return this.uri;
    }
}
