package org.ld4l.bib2lod.rdfconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Defines classes used by LD4L in the RDF output of the conversion process.
 * They may be in the LD4L namespace (the default) or in an external namespace.
 * The enum names don't indicate ontology (e.g., not FOAF_PERSON, etc.) in 
 * order to abstract away from the particular ontologies and classes used. If 
 * they change, it only requires a change here rather than in enum references.
 */
public enum Ld4lType {
    
    ANNOTATION(OntNamespace.OA, "Annotation"),
    CLASSIFICATION("Classification"),
    CONFERENCE("Conference"),
    DDC_SHELF_MARK("DdcShelfMark"),
    EVENT(OntNamespace.SCHEMA, "Event"),
    FAMILY("Family"),
    IDENTIFIER("Identifier"),
    INSTANCE("Instance"),
    ITEM("Item"),
    JURISDICTION("Jurisdiction"),
    LANGUAGE(OntNamespace.LINGVO, "Lingvo"),
    LCC_SHELF_MARK("LccShelfMark"),
    MEETING("Meeting"),
    NLM_SHELF_MARK("NlmShelfMark"),
    ORGANIZATION(OntNamespace.FOAF, "Organization"),
    PERSON(OntNamespace.FOAF, "Person"),
    PLACE(OntNamespace.PROV, "Location"),
    SHELF_MARK("ShelfMark"),
    TIME(OntNamespace.TIME, "TemporalEntity"),
    TITLE(OntNamespace.MADSRDF, "Title"),
    TOPIC("Topic"),
    UDC_SHELF_MARK("UdcShelfMark"),
    WORK("Work");
    

    private final OntNamespace namespace;
    private final String localname;
    private final String uri;
    private final String filename;
    private final String prefixed;
    private final String sparqlUri;
    private final Resource ontClass;

    Ld4lType(String localname) {
        // Default namespace for this enum type
        this(OntNamespace.LD4L, localname);
    }
    
    Ld4lType(OntNamespace namespace, String localname) {
        this.namespace = namespace;
        this.localname = localname;
        
        // Save as instance variables so don't recompute on each call.
        this.uri = namespace.uri() + localname;
        this.sparqlUri = "<" + this.uri + ">";
        
        String prefix = this.namespace.prefix();
        this.filename = prefix + this.localname; 
        this.prefixed = prefix + ":" + this.localname;

        // Create the Jena ontClass in the constructor to avoid repeated
        // entity creation; presumably a performance optimization, but should
        // test.
        this.ontClass = ResourceFactory.createResource(uri);

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
   
    private static final Map<String, Ld4lType> LOOKUP_BY_FILENAME = 
            new HashMap<String, Ld4lType>();
    
    static {
        for (Ld4lType type : Ld4lType.values()) {
            LOOKUP_BY_FILENAME.put(type.filename, type);
        }
    }
    
    public static Ld4lType typeForFilename(String filename) {
        String basename = FilenameUtils.getBaseName(filename);
        return LOOKUP_BY_FILENAME.get(basename);
    }

    public Resource ontClass() {
        return ontClass;
    }
    
}
