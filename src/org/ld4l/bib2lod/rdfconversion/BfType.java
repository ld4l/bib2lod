package org.ld4l.bib2lod.rdfconversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Defines classes used by Bibframe in the RDF input to the conversion process.
 */
public enum BfType {

    BF_AGENT("Agent", Ld4lType.AGENT),
    BF_ANNOTATION("Annotation", Ld4lType.ANNOTATION),
    BF_ARCHIVAL("Archival"),
    BF_ARRANGEMENT("Arrangement"),
    BF_AUDIO("Audio", Ld4lType.AUDIO),
    BF_AUTHORITY("Authority"),
    BF_CARTOGRAPHY("Cartography", Ld4lType.CARTOGRAPHY),
    BF_CATEGORY("Category"),
    BF_CLASSIFICATION("Classification", Ld4lType.CLASSIFICATION),
    BF_COLLECTION("Collection", Ld4lType.COLLECTION),
    BF_DATASET("Dataset", Ld4lType.DATASET),
    BF_ELECTRONIC("Electronic", Ld4lType.ELECTRONIC),
    BF_EVENT("Event", Ld4lType.EVENT),
    BF_EXPRESSION("Expression", Ld4lType.WORK),
    BF_FAMILY("Family", Ld4lType.FAMILY),
    BF_HELD_MATERIAL("HeldMaterial", Ld4lType.POLICY_SET),
    BF_HELD_ITEM("HeldItem", Ld4lType.ITEM),
    // Class is assigned in BfIdentifierConverter
    // BF_IDENTIFIER("Identifier", Ld4lType.IDENTIFIER), 
    BF_IDENTIFIER("Identifier"),   
    BF_INSTANCE("Instance", Ld4lType.INSTANCE),    
    BF_INTEGRATING("Integrating", Ld4lType.INTEGRATING_RESOURCE), 
    BF_INTENDED_AUDIENCE("IntendedAudience", Ld4lType.AUDIENCE),
    BF_JURISDICTION("Jurisdiction", Ld4lType.GOVERNMENT_ORGANIZATION),  
    BF_LANGUAGE("Language", Ld4lType.LANGUAGE),
    // ld4l:Manuscript has domain Item rather than Instance, so we don't want a
    // direct conversion.
    // BF_MANUSCRIPT("Manuscript", Ld4lType.MANUSCRIPT), 
    BF_MANUSCRIPT("Manuscript"), 
    BF_MEETING("Meeting", Ld4lType.MEETING),
    BF_MIXED_MATERIAL("MixedMaterial"),
    // ld4l:Monograph has domain Work rather than Instance, so we don't want a
    // direct conversion.
    // BF_MONOGRAPH("Monograph", Ld4lType.MONOGRAPH),
    BF_MONOGRAPH("Monograph"),
    BF_MOVING_IMAGE("MovingImage", Ld4lType.MOVING_IMAGE),
    BF_MULTIMEDIA("Multimedia", Ld4lType.MULTIMEDIA),
    // ld4l:MultipartMonograph has domain Work rather than Instance, so we 
    // don't want a direct conversion.
    // BF_MULTIPART_MONOGRAPH(
    //         "MultipartMonograph", Ld4lType.MULTIPART_MONOGRAPH),
    BF_MULTIPART_MONOGRAPH("MultipartMonograph"),
    BF_NOTATED_MOVEMENT("NotatedMovement", Ld4lType.NOTATED_MOVEMENT),
    BF_NOTATED_MUSIC("NotatedMusic", Ld4lType.NOTATED_MUSIC),
    BF_ORGANIZATION("Organization", Ld4lType.ORGANIZATION),                        
    BF_PERSON("Person", Ld4lType.PERSON),  
    // Don't add Provider mapping here; handled in BfProviderConverter. If the
    // mapping is also included here, we get a type statement to the Provision
    // superclass in addition to the Provision subclass.
    // BF_PROVIDER("Provider", Ld4lType.PROVISION),
    BF_PROVIDER("Provider"),
    BF_PLACE("Place", Ld4lType.PLACE),
    BF_PRINT("Print", Ld4lType.PRINT),
    BF_RELATOR("Relator"),
    BF_RESOURCE("Resource"),
    BF_REVIEW("Review", Ld4lType.ANNOTATION),
    // ld4l:Serial has domain Work rather than Instance, so we don't want a
    // direct conversion.
    // BF_SERIAL("Serial", Ld4lType.SERIAL),
    BF_SERIAL("Serial"),
    BF_STILL_IMAGE("StillImage", Ld4lType.STILL_IMAGE),
    BF_SUMMARY("Summary", Ld4lType.ANNOTATION),
    BF_TACTILE("Tactile", Ld4lType.TACTILE),
    BF_TEMPORAL("Temporal", Ld4lType.TIME),
    BF_TEXT("Text", Ld4lType.TEXT), 
    BF_THREE_DIMENSIONAL_OBJECT(
            "ThreeDimensionalObject", Ld4lType.THREE_DIMENSIONAL_OBJECT),
    BF_TITLE("Title", Ld4lType.TITLE),
    BF_TOPIC("Topic", Ld4lType.TOPIC),  
    BF_WORK("Work", Ld4lType.WORK),
    
    MADSRDF_AUTHORITY(OntNamespace.MADSRDF, "Authority", 
            Ld4lType.MADSRDF_AUTHORITY),
    MADSRDF_COMPLEX_SUBJECT(OntNamespace.MADSRDF, "ComplexSubject"),
    MADSRDF_CONFERENCE_NAME(OntNamespace.MADSRDF, "ConferenceName");
    
    private static final Logger LOGGER = LogManager.getLogger(BfType.class);
              
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
        for (BfType bfType : values()) {
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
    
    public static Map<Resource, BfType> typesForOntClasses() {
        return LOOKUP_BY_JENA_ONTCLASS;
    }
    
    public static List<BfType> authorities() {
        return Arrays.asList(
            BF_AGENT,
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
    
    public static boolean isAuthority(List<BfType> types) {
        
        /*
        for (BfType type : types) {
            if (authorities().contains(type)) {
                return true;
            }
        }
        return false;
        */
        
        return ! Collections.disjoint(authorities(), types);

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
    
    public static Map<Resource, Resource> typeMap(Map<BfType, Ld4lType> map) {

        Map<Resource, Resource> typeMap = new HashMap<Resource, Resource>();
        
        for (Map.Entry<BfType, Ld4lType> entry : map.entrySet()) {
            typeMap.put(
                    entry.getKey().ontClass, entry.getValue().ontClass());
        }
                
        return typeMap;
    }
    
    public static Map<Resource, Resource> typeMap(List<BfType> bfTypes) {
        
        Map<Resource, Resource> typeMap = new HashMap<Resource, Resource>();
        
        for (BfType bfType : bfTypes) {
            Ld4lType ld4lType = bfType.ld4lType;
            if (ld4lType != null) {
                typeMap.put(bfType.ontClass, ld4lType.ontClass());                    
            }
        }
                
        return typeMap;        
    }
    
    public static List<Resource> types(List<BfType> bfTypes) {
        
        List<Resource> resources = new ArrayList<Resource>();
        for (BfType type : bfTypes) {
            resources.add(type.ontClass);
        }
        
        return resources;        
    }
    
}
