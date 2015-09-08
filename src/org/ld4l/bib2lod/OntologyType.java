package org.ld4l.bib2lod;

public enum OntologyType {

    // Maybe create different type enums, one for bibframe, one for madsrdf
    // or miscellaneous, etc.
    
    BF_ANNOTATION(Namespace.BIBFRAME.uri(), "Annotation"),
    BF_FAMILY(Namespace.BIBFRAME.uri(), "Family"),
    BF_HELD_ITEM(Namespace.BIBFRAME.uri(), "HeldItem"),
    BF_IDENTIFIER(Namespace.BIBFRAME.uri(), "Identifier"),
    BF_INSTANCE(Namespace.BIBFRAME.uri(), "Instance"),
    BF_JURISDICTION(Namespace.BIBFRAME.uri(), "Jurisdiction"),
    BF_MEETING(Namespace.BIBFRAME.uri(), "Meeting"),
    BF_ORGANIZATION(Namespace.BIBFRAME.uri(), "Organization"),
    BF_PERSON(Namespace.BIBFRAME.uri(), "Person"),
    BF_PROVIDER(Namespace.BIBFRAME.uri(), "Provider"),
    BF_PLACE(Namespace.BIBFRAME.uri(), "Place"),
    BF_TITLE(Namespace.BIBFRAME.uri(), "Title"),
    BF_TOPIC(Namespace.BIBFRAME.uri(), "Topic"),  
    BF_WORK(Namespace.BIBFRAME.uri(), "Work"),
    
    MADSRDF_AUTHORITY(Namespace.MADSRDF.uri(), "Authority");

    private final String namespace;
    private final String localname;
    private final String uri;
    
    OntologyType(String namespace, String localname) {
        // Or should this be a Namespace?
        this.namespace = namespace;
        this.localname = localname;
        // Convenience field
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
