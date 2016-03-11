package org.ld4l.bib2lod.rdfconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Defines classes used by LD4L in the RDF output of the conversion process.
 * They may be in the LD4L namespace (the default) or in an external namespace.
 * The enum names don't indicate ontology (e.g., not FOAF_PERSON, etc.) in 
 * order to abstract away from the particular ontologies and classes used. If 
 * they change, it only requires a change here rather than in enum references.
 */
public enum Ld4lType {
    
    ABBREVIATED_TITLE("AbbreviatedTitle"),
    AGENT(OntNamespace.FOAF, "Agent"),
    ANNOTATION(OntNamespace.OA, "Annotation"),
    ANSI("Ansi"),
    AUDIENCE(OntNamespace.SCHEMA, "Audience"),
    AUDIO("Audio"),
    AUTHOR_CONTRIBUTION("AuthorContribution", "Author"),
    CARTOGRAPHY("Cartography"),
    CLASSIFICATION("Classification"),
    CODEN("Coden"),
    COLLECTION("Collection"),
    COMPOSER_CONTRIBUTION("ComposerContribution", "Composer"),
    CONDUCTOR_CONTRIBUTION("ConductorContribution", "Conductor"),
    CONFERENCE("Conference"),
    CONTRIBUTION("Contribution", "Contributor"),
    CREATOR_CONTRIBUTION("CreatorContribution", "Creator"),
    DATASET("Dataset"),
    DATE(OntNamespace.DC, "date"),
    DDC_SHELF_MARK("DdcShelfMark"),
    DISSERTATION_IDENTIFIER("DissertationIdentifier"),
    DISTRIBUTOR_PROVISION("DistributorProvision", "Distributor"),
    DOI("Doi"),
    EAN("Ean"),
    EDITOR_CONTRIBUTION("EditorContribution", "Editor"),
    ELECTRONIC("Electronic"),
    EVENT(OntNamespace.SCHEMA, "Event"),
    FAMILY("Family"),
    FINGERPRINT("Fingerprint"),
    GOVERNMENT_ORGANIZATION(OntNamespace.SCHEMA, "GovernmentOrganization"),
    HDL("Hdl"),
    IDENTIFIER("Identifier"),
    INSTANCE("Instance"),
    INTEGRATING_RESOURCE("IntegratingResource"),
    ISAN("Isan"),
    ISBN("Isbn"),
    ISBN10("Isbn10"),
    ISBN13("Isbn13"),
    ISMN("Ismn"),
    ISO("Iso"),
    ISSN("Issn"),
    ISSNL("IssnL"),
    ISSUE_NUMBER("IssueNumber"),
    ISTC("Istc"),
    ISWC("Iswc"),
    ITEM("Item"),
    JURISDICTION("Jurisdiction"),
    KEY_TITLE("KeyTitle"),
    LANGUAGE(OntNamespace.LINGVO, "Lingvo"),
    LC_OVERSEAS_ACQ_NUMBER("LcOverseasAcqNumber"),
    LCC_SHELF_MARK("LccShelfMark"),
    LCCN("Lccn"),
    LEGAL_DEPOSIT_NUMBER("LegalDepositNumber"),
    LOCAL_ILS_IDENTIFIER("LocalIlsIdentifier"),
    MADSRDF_AUTHORITY(OntNamespace.MADSRDF, "Authority"),
    MAIN_TITLE_ELEMENT(OntNamespace.MADSRDF, "MainTitleElement"),
    MANUFACTURER_PROVISION("ManufacturerProvision", "Manufacturer"),
    MANUSCRIPT("Manuscript"),
    MATRIX_NUMBER("MatrixNumber"),
    MEETING(OntNamespace.VIVO, "Meeting"),
    MONOGRAPH("Monograph"),
    MOVING_IMAGE("MovingImage"),
    MULTIMEDIA("Multimedia"),
    MULTIPART_MONOGRAPH("MultipartMonograph"),
    MUSIC_PLATE_NUMBER("MusicPlateNumber"),
    MUSIC_PUBLISHER_NUMBER("MusicPublisherNumber"),
    NARRATOR_CONTRIBUTION("NarratorContribution", "Narrator"),
    NBAN("Nban"),
    NBN("Nbn"),
    NLM_SHELF_MARK("NlmShelfMark"),
    NON_SORT_TITLE_ELEMENT(OntNamespace.MADSRDF, "NonSortElement"),
    NOTATED_MOVEMENT("NotatedMovement"),
    NOTATED_MUSIC("NotatedMusic"),
    OCLC_IDENTIFIER("OclcIdentifier"),
    ORGANIZATION(OntNamespace.FOAF, "Organization"),
    PART_NAME_TITLE_ELEMENT(OntNamespace.MADSRDF, "PartNameElement"),
    PART_NUMBER_TITLE_ELEMENT(OntNamespace.MADSRDF, "PartNumberElement"),
    PERFORMER_CONTRIBUTION("PerformerContribution", "Performer"),
    PERSON(OntNamespace.FOAF, "Person"),
    PLACE(OntNamespace.PROV, "Location"),
    POLICY_SET("PolicySet"),
    POSTAL_REGISTRATION_NUMBER("PostalRegistrationNumber"),
    PRINT("Print"),
    PRODUCER_PROVISION("ProducerProvision", "Producer"),
    PROVISION("Provision", "Provider"),
    PUBLISHER_NUMBER("PublisherNumber"),
    PUBLISHER_PROVISION("PublisherProvision", "Publisher"),
    SERIAL("Serial"),
    SHELF_MARK("ShelfMark"),
    SICI("Sici"),
    SOURCE_STATUS("SourceStatus"),
    STILL_IMAGE("StillImage"),
    STOCK_NUMBER("StockNumber"),
    STRN("Strn"),
    STUDY_NUMBER("StudyNumber"),
    SUBTITLE_ELEMENT(OntNamespace.MADSRDF, "SubTitleElement"),
    SYSTEM_NUMBER("SystemNumber"),
    TACTILE("Tactile"),
    TECHNICAL_REPORT_NUMBER("TechnicalReportNumber"),
    TEXT("Text"),
    TEXT_CONTENT(OntNamespace.CONTENT, "ContentAsText"),
    THREE_DIMENSIONAL_OBJECT("ThreeDimensionalObject"),
    TIME(OntNamespace.TIME, "TemporalEntity"),
    TITLE(OntNamespace.MADSRDF, "Title"),
    TITLE_ELEMENT(OntNamespace.MADSRDF, "TitleElement"),
    TOPIC("Topic"),
    UDC_SHELF_MARK("UdcShelfMark"),
    UPC("Upc"),
    VIDEO_RECORDING_NUMBER("VideoRecordingNumber"),
    WORK("Work");

    
    private static final Logger LOGGER = LogManager.getLogger(Ld4lType.class);           
    
    private final OntNamespace namespace;
    private final String localname;
    private final String uri;
    // Filename only used by factory classes, which are no longer in use.
    // private final String filename;
    private final String prefixed;
    private final String sparqlUri;
    private final Resource type;
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
        // this.filename = prefix + this.localname; 
        this.prefixed = prefix + ":" + this.localname;

        // Create the Jena type in the constructor to avoid repeated
        // entity creation; presumably a performance optimization, but should
        // test.
        this.type = ResourceFactory.createResource(uri);
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
    
//    public String filename() {
//        return filename;
//    }
    
    public String prefixed() {
        return prefixed; 
    }
    
    public String sparqlUri() {
        return sparqlUri;
    }
    
    public Resource type() {
        return type;
    }
   
//    private static final Map<String, Ld4lType> LOOKUP_BY_FILENAME = 
//            new HashMap<String, Ld4lType>();
    
    private static final Map<String, Ld4lType> LOOKUP_BY_LABEL = 
            new HashMap<String, Ld4lType>();
    
    private static final Map<String, Ld4lType> LOOKUP_BY_LOCAL_NAME = 
            new HashMap<String, Ld4lType>();
    
    static {
        for (Ld4lType type : Ld4lType.values()) {
            //LOOKUP_BY_FILENAME.put(type.filename, type);
            LOOKUP_BY_LABEL.put(type.label, type);
            LOOKUP_BY_LOCAL_NAME.put(type.localname,type);
        }
    }
    
//    public static Ld4lType typeForFilename(String filename) {
//        String basename = FilenameUtils.getBaseName(filename);
//        return LOOKUP_BY_FILENAME.get(basename);
//    }
    
    public static Ld4lType getByLabel(String label) {
        return LOOKUP_BY_LABEL.get(label);
    }
    
    public static Ld4lType getByLocalName(String localname) {
        return LOOKUP_BY_LOCAL_NAME.get(localname);
    }


    
}
