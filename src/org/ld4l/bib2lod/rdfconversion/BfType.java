package org.ld4l.bib2lod.rdfconversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Defines classes used by Bibframe in the RDF input to the conversion process.
 */
public enum BfType {

    BF_AGENT("Agent", Ld4lType.AGENT),
    BF_ANNOTATION("Annotation", Ld4lType.ANNOTATION),
    BF_ARCHIVAL("Archival"),
    BF_AUTHORITY("Authority"),
    BF_CATEGORY("Category"),
    BF_CLASSIFICATION("Classification", Ld4lType.CLASSIFICATION),
    BF_COLLECTION("Collection", Ld4lType.COLLECTION),
    BF_ELECTRONIC("Electronic", Ld4lType.ELECTRONIC),
    BF_EVENT("Event", Ld4lType.EVENT),
    BF_FAMILY("Family", Ld4lType.FAMILY),
    BF_HELD_ITEM("HeldItem", Ld4lType.ITEM),
    BF_IDENTIFIER("Identifier", Ld4lType.IDENTIFIER),    
    BF_INSTANCE("Instance", Ld4lType.INSTANCE),    
    BF_INTEGRATING("Integrating", Ld4lType.INTEGRATING_RESOURCE), 
    BF_JURISDICTION("Jurisdiction", Ld4lType.JURISDICTION),  
    BF_LANGUAGE("Language", Ld4lType.LANGUAGE),
    BF_MANUSCRIPT("Manuscript", Ld4lType.MANUSCRIPT),
    BF_MEETING("Meeting", Ld4lType.MEETING),
    BF_MONOGRAPH("Monograph", Ld4lType.MONOGRAPH),
    BF_MULTIPART_MONOGRAPH("MultipartMonograph", Ld4lType.MULTIPART_MONOGRAPH),
    BF_ORGANIZATION("Organization", Ld4lType.ORGANIZATION),                        
    BF_PERSON("Person", Ld4lType.PERSON),
    BF_PROVIDER("Provider"),
    BF_PLACE("Place", Ld4lType.PLACE),
    BF_PRINT("Print", Ld4lType.PRINT),
    BF_SERIAL("Print", Ld4lType.SERIAL),
    BF_TACTILE("Print", Ld4lType.TACTILE),
    BF_TEMPORAL("Temporal", Ld4lType.TIME),
    BF_TITLE("Title", Ld4lType.TITLE),
    BF_TOPIC("Topic", Ld4lType.TOPIC),  
    BF_WORK("Work", Ld4lType.WORK),
    
    MADSRDF_AUTHORITY(OntNamespace.MADSRDF, "Authority"),
    MADSRDF_COMPLEX_SUBJECT(OntNamespace.MADSRDF, "ComplexSubject"),
    MADSRDF_CONFERENCE_NAME(OntNamespace.MADSRDF, "ConferenceName");
    

    private final OntNamespace namespace;
    private final String localname;
    private final String uri;
    private final String filename;
    private final String prefixed;
    private final String sparqlUri;
    private final Resource ontClass;
    private final Ld4lType ld4lType;

    BfType(String localname) {
        // Assign default namespace to this enum value
        // No corresponding Ld4lType for this enum value
        this(OntNamespace.BIBFRAME, localname, null);
    }
    
    BfType(OntNamespace namespace, String localname) {
        // No corresponding Ld4lType for this enum value
        this(namespace, localname, null);
    }
    
    BfType(String localname, Ld4lType ld4lType) {
        // Assign default namespace to this enum value
        this(OntNamespace.BIBFRAME, localname, ld4lType);
    }
    
    BfType(OntNamespace namespace, String localname, Ld4lType ld4lType) {
        this.namespace = namespace;
        this.localname = localname;
        this.ld4lType = ld4lType;
        
        // Save as instance variables so don't recompute on each call.
        this.uri = namespace.uri() + localname;
        this.sparqlUri = "<" + this.uri + ">";
        
        String prefix = this.namespace.prefix();
        this.filename = prefix + this.localname; 
        this.prefixed = prefix + ":" + this.localname;

        // Create the Jena ontClass in the constructor to avoid repeated
        // entity creation; presumably a performance optimization, but should
        // test.
        this.ontClass = ResourceFactory.createResource(this.uri);

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
   
    private static final Map<String, BfType> LOOKUP_BY_FILENAME = 
            new HashMap<String, BfType>();
    private static final Map<Resource, BfType> LOOKUP_BY_JENA_ONTCLASS = 
            new HashMap<Resource, BfType>();
    private static final Map<Resource, Resource> TYPE_MAP = 
            new HashMap<Resource, Resource>();
    
    static {
        for (BfType bfType : BfType.values()) {
            LOOKUP_BY_FILENAME.put(bfType.filename, bfType);
            if (bfType.ld4lType != null) {
                TYPE_MAP.put(bfType.ontClass, bfType.ld4lType.ontClass());
            }
            LOOKUP_BY_JENA_ONTCLASS.put(bfType.ontClass, bfType);
        }
    }
    
    public static BfType typeForFilename(String filename) {
        String basename = FilenameUtils.getBaseName(filename);
        return LOOKUP_BY_FILENAME.get(basename);
    }
    
    public static BfType typeForOntClass(Resource ontClass) {
        return LOOKUP_BY_JENA_ONTCLASS.get(ontClass);
    }
    
    public static List<BfType> authorities() {
        return Arrays.asList(
            BF_AUTHORITY,
            BF_FAMILY,
            BF_JURISDICTION,
            BF_MEETING,
            BF_ORGANIZATION,
            BF_PERSON,
            BF_PLACE,
            BF_TEMPORAL,
            BF_TOPIC);
    }
    
    public boolean isAuthority() {
        return authorities().contains(this);
    }
    
    public Resource ontClass() {
        return ontClass;
    }
    
    public Ld4lType ld4lType() {
        return ld4lType;
    }
    
    public static Map<Resource, Resource> typeMap() {
        return TYPE_MAP;
    }
    
    public static Map<Resource, Resource> typeMap(List<BfType> bfTypes) {

        Map<Resource, Resource> typeMap = new HashMap<Resource, Resource>();
        
        if (bfTypes != null) {
            for (BfType type : bfTypes) {
                if (type.ld4lType != null) {
                    typeMap.put(type.ontClass, type.ld4lType.ontClass());
                }
            }
        }
        
        return typeMap;
    }
    
}
