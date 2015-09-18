package org.ld4l.bib2lod.rdfconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.ld4l.bib2lod.rdfconversion.typededuping.BfPersonDeduper;

public enum OntologyType {

    // Maybe create different type enums, one for bibframe, one for madsrdf
    // or miscellaneous, etc.
    
    BF_ANNOTATION(Namespace.BIBFRAME, "Annotation"),
    BF_FAMILY(Namespace.BIBFRAME, "Family"),
    BF_HELD_ITEM(Namespace.BIBFRAME, "HeldItem"),
    BF_IDENTIFIER(Namespace.BIBFRAME, "Identifier"),
    BF_INSTANCE(Namespace.BIBFRAME, "Instance"),
    BF_JURISDICTION(Namespace.BIBFRAME, "Jurisdiction"),
    BF_MEETING(Namespace.BIBFRAME, "Meeting"),
    BF_ORGANIZATION(Namespace.BIBFRAME, "Organization"),
    BF_PERSON(Namespace.BIBFRAME, "Person"),
    BF_PROVIDER(Namespace.BIBFRAME, "Provider"),
    BF_PLACE(Namespace.BIBFRAME, "Place"),
    BF_TITLE(Namespace.BIBFRAME, "Title"),
    BF_TOPIC(Namespace.BIBFRAME, "Topic"),  
    BF_WORK(Namespace.BIBFRAME, "Work"),
    
    MADSRDF_AUTHORITY(Namespace.MADSRDF, "Authority");

    private final Namespace namespace;
    private final String localname;
    private final String uri;
    private final String filename;
    
    OntologyType(Namespace namespace, String localname) {
        this.namespace = namespace;
        this.localname = localname;
        // Convenience field
        this.uri = namespace.uri() + localname;
        
        String namespaceUri = namespace.uri();
        
        // Last char of namespace can be / or #
        String lastChar = namespaceUri.substring(namespaceUri.length() - 1);
        String ln = StringUtils.substringAfterLast(this.uri, lastChar);
        String ns = StringUtils.substringBeforeLast(this.uri, lastChar) + lastChar;
        String prefix = Namespace.getNsPrefix(ns);
        this.filename = prefix + ln;   
        
        String className = StringUtils.capitalize(this.namespace.prefix() + 
                this.localname + "Deduper");
                
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
    
    public String filename() {
        return this.filename;
    }
    
    public String prefixedForm() {
        return this.namespace.prefix() + ":" + this.localname;
    }
    
    public String sparqlUri() {
        return "<" + this.uri + ">";
    }
    
    private static final Map<String, OntologyType> LOOKUP_BY_FILENAME = 
            new HashMap<String, OntologyType>();
    

    static {
        for (OntologyType type : OntologyType.values()) {
            LOOKUP_BY_FILENAME.put(type.filename, type);
        }
    }
    
    public static OntologyType getByFilename(String filename) {
        String basename = FilenameUtils.getBaseName(filename);
        return LOOKUP_BY_FILENAME.get(basename);
    }

}
