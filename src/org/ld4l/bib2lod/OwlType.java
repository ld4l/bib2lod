package org.ld4l.bib2lod;

import org.ld4l.bib2lod.Ontology;

public enum OwlType {

    PERSON (Ontology.BIBFRAME_LD4L.namespace(), "Person"),
    WORK (Ontology.BIBFRAME_LD4L.namespace(), "Work"),
    INSTANCE (Ontology.BIBFRAME_LD4L.namespace(), "Instance"),
    ORGANIZATION (Ontology.FOAF.namespace(), "Organization"),
    SUBJECT (Ontology.DCTERMS.namespace(), "Subject");

    private final String uri;

    OwlType(String namespace, String localname) {
        this.uri = namespace + localname;
    }
    
    public String uri() {
        return this.uri;
    }

}
