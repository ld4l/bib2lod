package org.ld4l.bib2lod.rdfconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;

public enum OntType {

    // Maybe create different type enums, one for bibframe, one for madsrdf
    // or miscellaneous, etc.? For now, no need.
    
    BF_ANNOTATION(OntNamespace.BIBFRAME, "Annotation"),
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
    MADSRDF_COMPLEX_SUBJECT(OntNamespace.MADSRDF, "ComplexSubject");
    

    private final OntNamespace namespace;
    private final String localname;
    private final String uri;
    private final String filename;
    
    OntType(OntNamespace namespace, String localname) {
        this.namespace = namespace;
        this.localname = localname;
        this.uri = namespace.uri() + localname;
        this.filename = this.namespace.prefix() + this.localname; 
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
    
    public String prefixedForm() {
        return namespace.prefix() + ":" + localname;
    }
    
    public String sparqlUri() {
        return "<" + uri + ">";
    }
   
    private static final Map<String, OntType> LOOKUP_BY_FILENAME = 
            new HashMap<String, OntType>();
    
    static {
        for (OntType type : OntType.values()) {
            LOOKUP_BY_FILENAME.put(type.filename, type);
        }
    }
    
    public static OntType getByFilename(String filename) {
        String basename = FilenameUtils.getBaseName(filename);
        return LOOKUP_BY_FILENAME.get(basename);
    }

}
