package org.ld4l.bib2lod.rdfconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.ld4l.bib2lod.rdfconversion.typededuping.BfAgentDeduper;
import org.ld4l.bib2lod.rdfconversion.typededuping.BfInstanceDeduper;
import org.ld4l.bib2lod.rdfconversion.typededuping.BfTopicDeduper;

public enum OntType {

    // Maybe create different type enums, one for bibframe, one for madsrdf
    // or miscellaneous, etc.? For now, no need.
    
    BF_ANNOTATION(OntNamespace.BIBFRAME, "Annotation"),
    BF_FAMILY(OntNamespace.BIBFRAME, "Family", BfAgentDeduper.class),
    BF_HELD_ITEM(OntNamespace.BIBFRAME, "HeldItem"),
    BF_IDENTIFIER(OntNamespace.BIBFRAME, "Identifier"),
    BF_INSTANCE(OntNamespace.BIBFRAME, "Instance", BfInstanceDeduper.class),    
    BF_JURISDICTION(
            OntNamespace.BIBFRAME, "Jurisdiction", BfAgentDeduper.class),
    BF_MEETING(OntNamespace.BIBFRAME, "Meeting"),
    BF_ORGANIZATION(
            OntNamespace.BIBFRAME, "Organization", BfAgentDeduper.class),            
    BF_PERSON(OntNamespace.BIBFRAME, "Person", BfAgentDeduper.class),
    BF_PROVIDER(OntNamespace.BIBFRAME, "Provider"),
    BF_PLACE(OntNamespace.BIBFRAME, "Place"),
    BF_TITLE(OntNamespace.BIBFRAME, "Title"),
    BF_TOPIC(OntNamespace.BIBFRAME, "Topic", BfTopicDeduper.class),  
    BF_WORK(OntNamespace.BIBFRAME, "Work"),
    
    MADSRDF_AUTHORITY(OntNamespace.MADSRDF, "Authority");
    

    private final OntNamespace namespace;
    private final String localname;
    private final String uri;
    private final String filename;
    private Class<?> deduper;
    
    OntType(OntNamespace namespace, String localname) {
        this.namespace = namespace;
        this.localname = localname;
        this.uri = namespace.uri() + localname;
        this.filename = this.namespace.prefix() + this.localname; 
        this.deduper = null;
    }

    OntType(OntNamespace namespace, String localname, Class<?> deduper) {   
        this(namespace, localname);
        this.deduper = deduper;
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
   
    public Class<?> deduper() {
        return deduper;
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
