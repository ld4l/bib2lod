package org.ld4l.bib2lod.rdfconversion;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


/**
 * Define properties used by LD4L in the RDF output of the conversion process.
 * They may be in the LD4L namespace (the default) or in an external namespace.
 * The enum names don't indicate ontology (e.g., not FOAF_NAME, etc.) in 
 * order to abstract away from the particular ontologies and properties used. 
 * If they change, it only requires a change here rather than in enum 
 * references.
 */
public enum Ld4lProperty {

    ANNOTATED_AT(OntNamespace.OA, "annotatedAt"),
    ANNOTATED_BY(OntNamespace.OA, "annotatedBy"),
    AT_LOCATION(OntNamespace.PROV, "atLocation"),
    BARCODE("barcode"),
    BIRTHDATE(OntNamespace.SCHEMA, "birthDate"),
    CHARS(OntNamespace.CONTENT, "chars"),
    DATE(OntNamespace.DCTERMS, "date"),
    DEATHDATE(OntNamespace.SCHEMA, "deathDate"),
    DIMENSIONS("dimensions"),
    EXTENT("extent"),
    FOLLOWS("follows"),
    HAS_AGENT(OntNamespace.PROV, "agent"),
    HAS_ANNOTATION("hasAnnotation"),
    HAS_BODY(OntNamespace.OA, "hasBody"),
    HAS_CREATOR(OntNamespace.DCTERMS, "creator"),
    HAS_CONTRIBUTION("hasContribution"),
    HAS_EXPRESSION("hasExpression"),
    HAS_GENRE("hasGenre"),
    HAS_LANGUAGE(OntNamespace.DCTERMS, "language"),
    HAS_LOCATION(OntNamespace.PROV, "atLocation"),
    HAS_ORIGINAL_LANGUAGE("hasOriginalLanguage"),
    HAS_ORIGINAL_VERSION("hasOriginalVersion"),
    HAS_PART(OntNamespace.DCTERMS, "hasPart"),
    HAS_PREFERRED_TITLE("hasPreferredTitle"),
    HAS_PROVISION("hasProvision"),
    HAS_REPRODUCTION("hasReproduction"),
    HAS_SHELF_MARK("hasShelfMark"), // or hasLocator
    HAS_SUBJECT(OntNamespace.DCTERMS, "subject"),
    HAS_TARGET(OntNamespace.OA, "hasTarget"),
    HAS_TITLE("hasTitle"),
    IDENTIFIED_BY("identifiedBy"),
    IDENTIFIED_BY_AUTHORITY(OntNamespace.MADSRDF, "isIdentifiedByAuthority"),           
    IDENTIFIES_RWO(OntNamespace.MADSRDF, "identifiesRWO"),
    ILLUSTRATION_NOTE("illustrationNote"),
    IS_EXPRESSION_OF("isExpressionOf"),
    IS_HOLDING_FOR("isHoldingFor"),
    IS_INSTANCE_OF("isInstanceOf"),
    IS_PART_OF(OntNamespace.DCTERMS, "isPartOf"),
    IS_SUBJECT_OF("isSubjectOf"),
    LABEL(OntNamespace.RDFS, "label"),
    LEGACY_FORM_DESIGNATION(OntNamespace.LEGACY, "formDesignation"),
    LEGACY_MUSIC_KEY(OntNamespace.LEGACY, "musicKey"),
    LEGACY_PROVIDER_ROLE(OntNamespace.LEGACY, "providerRole"),
    LEGACY_PROVIDER_STATEMENT(OntNamespace.LEGACY, "providerStatement"),
    LEGACY_SUPPLEMENTARY_CONTENT_NOTE(OntNamespace.LEGACY, "supplementaryContentNote"),
    MADSRDF_AUTHORITATIVE_LABEL(OntNamespace.MADSRDF, "authoritativeLabel"),
    MADSRDF_IS_MEMBER_OF_MADS_SCHEME(
            OntNamespace.MADSRDF, "isMemberOfMADSScheme"),
    NAME(OntNamespace.FOAF, "name"),
    OWL_SAME_AS(OntNamespace.OWL, "sameAs"),
    PRECEDES("precedes"),
    PREFERRED_LABEL(OntNamespace.SKOS, "prefLabel"),
    RANK(OntNamespace.VIVO, "rank"),
    RELATED(OntNamespace.DCTERMS, "relation"),
    TRANSLATED_AS("translatedAs"),
    TRANSLATES("translates"),
    VALUE(OntNamespace.RDF, "value");
    
    private static final Logger LOGGER = 
            LogManager.getLogger(Ld4lProperty.class);
            
    
    private final OntNamespace namespace;
    private final String localname;
    private final String uri;
    private final String prefixed;
    private final Property property;
    
    Ld4lProperty(String localname) {
        // Default namespace for this enum type.
        this(OntNamespace.LD4L, localname);
    }
    
    Ld4lProperty(OntNamespace namespace, String localname) {
        // Or should this be a Namespace?
        this.namespace = namespace;
        this.localname = localname;
        this.uri = namespace.uri() + localname;
        
        String prefix = this.namespace.prefix();
        this.prefixed = prefix + ":" + this.localname;
        
        // Create the Jena property in the constructor to avoid repeated
        // entity creation; presumably a performance optimization, but should
        // test.
        this.property = ResourceFactory.createProperty(uri);
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
    
    public String prefixed() {
        return prefixed;
    }
    
    public String sparqlUri() {
        return "<" + uri + ">";
    }
    
    public Property property() {
        return property;
     }
}
