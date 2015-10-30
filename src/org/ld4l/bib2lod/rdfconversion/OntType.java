package org.ld4l.bib2lod.rdfconversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

public enum OntType {

    // Maybe create different type enums, one for bibframe, one for madsrdf
    // or miscellaneous, etc.? For now, no need.
    
    BF_ANNOTATION(OntNamespace.BIBFRAME, "Annotation"),
    BF_CLASSIFICATION(OntNamespace.BIBFRAME, "Classification"),
    BF_EVENT(OntNamespace.BIBFRAME, "Event"),
    BF_FAMILY(OntNamespace.BIBFRAME, "Family"),
    BF_HELD_ITEM(OntNamespace.BIBFRAME, "HeldItem"),
    BF_IDENTIFIER(OntNamespace.BIBFRAME, "Identifier"),
    BF_INSTANCE(OntNamespace.BIBFRAME, "Instance"),    
    BF_JURISDICTION(OntNamespace.BIBFRAME, "Jurisdiction"),            
    BF_MEETING(OntNamespace.BIBFRAME, "Meeting"),
    BF_ORGANIZATION(OntNamespace.BIBFRAME, "Organization"),                        
    BF_PERSON(OntNamespace.BIBFRAME, "Person"),
    BF_PROVIDER(OntNamespace.BIBFRAME, "Provider"),
    BF_PLACE(OntNamespace.BIBFRAME, "Place"),
    BF_TITLE(OntNamespace.BIBFRAME, "Title"),
    BF_TOPIC(OntNamespace.BIBFRAME, "Topic"),  
    BF_WORK(OntNamespace.BIBFRAME, "Work"),
    
    MADSRDF_AUTHORITY(OntNamespace.MADSRDF, "Authority"),
    MADSRDF_COMPLEX_SUBJECT(OntNamespace.MADSRDF, "ComplexSubject"),
    
    // The OntType names below don't indicate ontology, to abstract away from 
    // the particular ontology and class used.
    EVENT(OntNamespace.SCHEMA, "Event"),
    FAMILY(OntNamespace.LD4L, "Family"),
    ITEM(OntNamespace.LD4L, "Item"),
    ORGANIZATION(OntNamespace.FOAF, "Organization"),
    PERSON(OntNamespace.FOAF, "Person"),
    PLACE(OntNamespace.PROV, "Location");
    

    private final OntNamespace namespace;
    private final String localname;
    private final String uri;
    private final String filename;
    private final String prefixed;
    private final String sparqlUri;
    
    OntType(OntNamespace namespace, String localname) {
        this.namespace = namespace;
        this.localname = localname;
        
        // Save as instance variables so don't recompute on each call.
        this.uri = namespace.uri() + localname;
        this.sparqlUri = "<" + this.uri + ">";
        
        String prefix = this.namespace.prefix();
        this.filename = prefix + this.localname; 
        this.prefixed = prefix + ":" + this.localname;

    }

    public OntNamespace namespace() {
        return namespace;
    }
    
    public String namespaceUri() {
        return namespace.uri();
    }

    public String localname() {
        return localname;
    }
    
    public String uri() {
        return uri;
    }
    
    public String filename() {
        return filename;
    }
    
    public String prefixed() {
        return prefixed; 
    }
    
    public String sparqlUri() {
        return sparqlUri;
    }
   
    private static final Map<String, OntType> LOOKUP_BY_FILENAME = 
            new HashMap<String, OntType>();
    
    static {
        for (OntType type : OntType.values()) {
            LOOKUP_BY_FILENAME.put(type.filename, type);
        }
    }
    
    public static OntType typeForFilename(String filename) {
        String basename = FilenameUtils.getBaseName(filename);
        return LOOKUP_BY_FILENAME.get(basename);
    }
    
    public static List<OntType> authorities() {
        return Arrays.asList(
            BF_FAMILY,
            BF_JURISDICTION,
            BF_MEETING,
            BF_ORGANIZATION,
            BF_PERSON,
            BF_PLACE,
            BF_TOPIC);
    }

}
