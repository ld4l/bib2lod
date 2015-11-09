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

    // Maybe create different type enums, one for bibframe, one for madsrdf
    // or miscellaneous, etc.? For now, no need.
    
    BF_ANNOTATION("Annotation"),
    BF_CLASSIFICATION("Classification"),
    BF_EVENT("Event"),
    BF_FAMILY("Family"),
    BF_HELD_ITEM("HeldItem"),
    BF_IDENTIFIER("Identifier"),
    BF_INSTANCE("Instance"),    
    BF_JURISDICTION("Jurisdiction"),  
    BF_LANGUAGE("Language"),
    BF_MEETING("Meeting"),
    BF_ORGANIZATION("Organization"),                        
    BF_PERSON("Person"),
    BF_PROVIDER("Provider"),
    BF_PLACE("Place"),
    BF_TEMPORAL("Temporal"),
    BF_TITLE("Title"),
    BF_TOPIC("Topic"),  
    BF_WORK("Work"),
    
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

    BfType(String localname) {
        // Default namespace for this enum type
        this(OntNamespace.BIBFRAME, localname);
    }
    
    BfType(OntNamespace namespace, String localname) {
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
    
    static {
        for (BfType type : BfType.values()) {
            LOOKUP_BY_FILENAME.put(type.filename, type);
        }
    }
    
    public static BfType typeForFilename(String filename) {
        String basename = FilenameUtils.getBaseName(filename);
        return LOOKUP_BY_FILENAME.get(basename);
    }
    
    public static List<BfType> authorities() {
        return Arrays.asList(
            BF_FAMILY,
            BF_JURISDICTION,
            BF_MEETING,
            BF_ORGANIZATION,
            BF_PERSON,
            BF_PLACE,
            BF_TEMPORAL,
            BF_TOPIC);
    }
    
    public Resource ontClass() {
        return ontClass;
    }
    
}
