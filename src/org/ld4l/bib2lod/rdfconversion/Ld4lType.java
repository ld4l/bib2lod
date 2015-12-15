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
    
    AGENT(OntNamespace.FOAF, "Agent"),
    ANNOTATION(OntNamespace.OA, "Annotation"),
    AUDIENCE(OntNamespace.SCHEMA, "Audience"),
    AUDIO("Audio"),
    AUTHOR_CONTRIBUTION("AuthorContribution", "Author"),
    CARTOGRAPHY("Cartography"),
    CLASSIFICATION("Classification"),
    COLLECTION("Collection"),
    COMPOSER_CONTRIBUTION("ComposerContribution", "Composer"),
    CONDUCTOR_CONTRIBUTION("ConductorContribution", "Conductor"),
    CONFERENCE("Conference"),
    CONTRIBUTION("Contribution", "Contributor"),
    CREATOR_CONTRIBUTION("CreatorContribution", "Creator"),
    DATASET("Dataset"),
    DDC_SHELF_MARK("DdcShelfMark"),
    DISTRIBUTOR_PROVISION("DistributorProvision", "Distributor"),
    EDITOR_CONTRIBUTION("EditorContribution", "Editor"),
    ELECTRONIC("Electronic"),
    EVENT(OntNamespace.SCHEMA, "Event"),
    FAMILY("Family"),
    GOVERNMENT_ORGANIZATION(OntNamespace.SCHEMA, "GovernmentOrganization"),
    IDENTIFIER("Identifier"),
    INSTANCE("Instance"),
    INTEGRATING_RESOURCE("IntegratingResource"),
    ITEM("Item"),
    JURISDICTION("Jurisdiction"),
    LANGUAGE(OntNamespace.LINGVO, "Lingvo"),
    LOCAL_ILS_IDENTIFIER("LocalIlsIdentifier"),
    LCC_SHELF_MARK("LccShelfMark"),
    MAIN_TITLE_ELEMENT(OntNamespace.MADSRDF, "MainTitleElement"),
    MANUFACTURER_PROVISION("ManufacturerProvision", "Manufacturer"),
    MANUSCRIPT("Manuscript"),
    MEETING(OntNamespace.VIVO, "Meeting"),
    MONOGRAPH("Monograph"),
    MOVING_IMAGE("MovingImage"),
    MULTIMEDIA("Multimedia"),
    MULTIPART_MONOGRAPH("MultipartMonograph"),
    NARRATOR_CONTRIBUTION("NarratorContribution", "Narrator"),
    NLM_SHELF_MARK("NlmShelfMark"),
    NON_SORT_TITLE_ELEMENT(OntNamespace.MADSRDF, "NonSortElement"),
    NOTATED_MOVEMENT("NotatedMovement"),
    NOTATED_MUSIC("NotatedMusic"),
    ORGANIZATION(OntNamespace.FOAF, "Organization"),
    PART_NAME_TITLE_ELEMENT(OntNamespace.MADSRDF, "PartNameElement"),
    PERFORMER_CONTRIBUTION("PerformerContribution", "Performer"),
    PERSON(OntNamespace.FOAF, "Person"),
    PLACE(OntNamespace.PROV, "Location"),
    POLICY_SET("PolicySet"),
    PRINT("Print"),
    PRODUCER_PROVISION("ProducerProvision", "Producer"),
    PROVISION("Provision", "Provider"),
    PUBLISHER_PROVISION("PublisherProvision", "Publisher"),
    SERIAL("Serial"),
    SHELF_MARK("ShelfMark"),
    STILL_IMAGE("StillImage"),
    SUBTITLE_ELEMENT(OntNamespace.MADSRDF, "SubTitleElement"),
    TACTILE("Tactile"),
    TEXT("Text"),
    TEXT_CONTENT(OntNamespace.CONTENT, "ContentAsText"),
    THREE_DIMENSIONAL_OBJECT("ThreeDimensionalObject"),
    TIME(OntNamespace.TIME, "TemporalEntity"),
    TITLE(OntNamespace.MADSRDF, "Title"),
    TITLE_ELEMENT(OntNamespace.MADSRDF, "TitleElement"),
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
    private String label;

    Ld4lType(String localname) {
        // Default namespace for this enum type
        this(OntNamespace.LD4L, localname);
    }
    
    Ld4lType(String localname, String label) {
        this(OntNamespace.LD4L, localname);
        this.label = label;
    }
    
    Ld4lType(OntNamespace namespace, String localname) {
        this.namespace = namespace;
        this.localname = localname;
        this.label = null;
        
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
    
    public String label() {
        return label;
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
