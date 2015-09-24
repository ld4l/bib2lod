package org.ld4l.bib2lod.rdfconversion;


public enum OntologyProperty {

    BF_AUTHORIZED_ACCESS_POINT(Namespace.BIBFRAME, "authorizedAccessPoint"),
    BF_HAS_AUTHORITY(Namespace.BIBFRAME, "hasAuthority"),
    BF_IDENTIFIER(Namespace.BIBFRAME, "identifier"),
    BF_IDENTIFIER_VALUE(Namespace.BIBFRAME, "identifierValue"),
    BF_LABEL(Namespace.BIBFRAME, "label"),
    BF_SYSTEM_NUMBER(Namespace.BIBFRAME, "systemNumber"),
    MADSRDF_AUTHORITATIVE_LABEL(Namespace.MADSRDF, "authoritativeLabel");

    
    private final Namespace namespace;
    private final String localname;
    private final String uri;
    
    OntologyProperty(Namespace namespace, String localname) {
        // Or should this be a Namespace?
        this.namespace = namespace;
        this.localname = localname;
        this.uri = namespace.uri() + localname;
    }
    
    public Namespace namespace() {
        return this.namespace;
    }
    
    public String namespaceUri() {
        return this.namespace.uri();
    }
    
    public String localname() {
        return this.localname;
    }
    
    public String uri() {
        return this.uri;
    }
    
    public String prefixedForm() {
        return this.namespace.prefix() + ":" + this.localname;
    }
    
    public String sparqlUri() {
        return "<" + this.uri + ">";
    }
}
