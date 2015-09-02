package org.ld4l.bib2lod;

public enum OntologyType {

    ANNOTATION (Namespace.BIBFRAME.uri(), "Annotation"),
    FAMILY (Namespace.BIBFRAME.uri(), "Family"),
    HELD_ITEM (Namespace.BIBFRAME.uri(), "HeldItem"),
    INSTANCE (Namespace.BIBFRAME.uri(), "Instance"),
    JURISDICTION (Namespace.BIBFRAME.uri(), "Jurisdiction"),
    MEETING (Namespace.BIBFRAME.uri(), "Meeting"),
    ORGANIZATION (Namespace.BIBFRAME.uri(), "Organization"),
    PERSON (Namespace.BIBFRAME.uri(), "Person"),
    PROVIDER (Namespace.BIBFRAME.uri(), "Provider"),
    PLACE (Namespace.BIBFRAME.uri(), "Place"),
    TITLE (Namespace.BIBFRAME.uri(), "Title"),
    TOPIC (Namespace.BIBFRAME.uri(), "Topic"),  
    WORK (Namespace.BIBFRAME.uri(), "Work");

    private final String namespace;
    private final String localname;
    private final String uri;
    
    OntologyType(String namespace, String localname) {
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
