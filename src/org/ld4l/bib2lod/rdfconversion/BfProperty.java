                                                               package org.ld4l.bib2lod.rdfconversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

/**
 * Defines properties used by Bibframe in the RDF input to the conversion
 * process.
 * @author rjy7
 *
 */
public enum BfProperty {

    BF_ANNOTATES("annotates", Ld4lProperty.HAS_TARGET),
    BF_ANNOTATION_ASSERTED_BY("annotationAssertedBy", 
            Ld4lProperty.ANNOTATED_BY),
    BF_ANNOTATION_BODY("annotationBody", Ld4lProperty.HAS_BODY),
    BF_ASSERTION_DATE("assertionDate", Ld4lProperty.ANNOTATED_AT),
    BF_ANNOTATION_SOURCE("annotationSource", Ld4lProperty.HAS_CREATOR),
    BF_AUTHORITY_SOURCE("authoritySource"),
    BF_AUTHORIZED_ACCESS_POINT("authorizedAccessPoint"),
    BF_BARCODE("barcode", Ld4lProperty.BARCODE),
    BF_CHANGE_DATE("changeDate"),
    BF_CONTRIBUTOR("contributor", Ld4lProperty.HAS_CONTRIBUTION),
    BF_CREATOR("creator", Ld4lProperty.HAS_CONTRIBUTION),
    // LD4L uses the derivedFrom predicate to relate Titles to Titles and
    // Works to Works, but the Bibframe property is used to relate a Work or 
    // Instance to a marcxml record, so should be removed.
    BF_DERIVED_FROM("derivedFrom"),
    BF_DESCRIPTION_CONVENTIONS("descriptionConventions"),
    BF_DESCRIPTION_MODIFIER("descriptionModifier"),
    BF_DESCRIPTION_SOURCE("descriptionSource"),
    BF_DIMENSIONS("dimensions", Ld4lProperty.DIMENSIONS),
    BF_DISTRIBUTION("distribution", Ld4lProperty.HAS_PROVISION),
    BF_EVENT_PLACE("eventPlace", Ld4lProperty.HAS_LOCATION),
    BF_EXTENT("extent", Ld4lProperty.EXTENT),
    BF_GENERATION_PROCESS("generationProcess"),
    BF_HAS_ANNOTATION("hasAnnotation", Ld4lProperty.HAS_ANNOTATION),
    BF_HAS_AUTHORITY("hasAuthority", Ld4lProperty.IDENTIFIED_BY_AUTHORITY),
    BF_HAS_PART("hasPart", Ld4lProperty.HAS_PART),
    BF_HOLDING_FOR("holdingFor", Ld4lProperty.IS_HOLDING_FOR),
    BF_IDENTIFIER("identifier", Ld4lProperty.IDENTIFIED_BY),
    BF_IDENTIFIER_SCHEME("identifierScheme"),
    BF_IDENTIFIER_VALUE("identifierValue", Ld4lProperty.VALUE),
    BF_ILLUSTRATION_NOTE("illustrationNote", Ld4lProperty.ILLUSTRATION_NOTE),
    BF_INSTANCE_OF("instanceOf", Ld4lProperty.IS_INSTANCE_OF),
    BF_INSTANCE_TITLE("instanceTitle", Ld4lProperty.HAS_TITLE),
    BF_ITEM_ID("itemId", Ld4lProperty.IDENTIFIED_BY),
    BF_LABEL("label", Ld4lProperty.LABEL),
    BF_LANGUAGE("language", Ld4lProperty.HAS_LANGUAGE),
    BF_LANGUAGE_OF_PART("languageOfPart", Ld4lProperty.LABEL), 
    BF_LANGUAGE_OF_PART_URI("languageOfPartUri"),
    BF_MANUFACTURE("manufacture", Ld4lProperty.HAS_PROVISION),
    BF_MODE_OF_ISSUANCE("modeOfIssuance"),
    BF_PART_OF("partOf", Ld4lProperty.IS_PART_OF),
    BF_PRECEDED_BY("precededBy", Ld4lProperty.FOLLOWS),
    BF_PRODUCTION("production", Ld4lProperty.HAS_PROVISION),
    BF_PROVIDER("provider", Ld4lProperty.HAS_PROVISION),
    BF_PROVIDER_DATE("providerDate", Ld4lProperty.DATE),
    BF_PROVIDER_NAME("providerName", Ld4lProperty.HAS_AGENT),
    BF_PROVIDER_PLACE("providerPlace", Ld4lProperty.AT_LOCATION),
    BF_PROVIDER_ROLE("providerRole", Ld4lProperty.LEGACY_PROVIDER_ROLE),
    BF_PROVIDER_STATEMENT("providerStatement", 
            Ld4lProperty.LEGACY_PROVIDER_STATEMENT),
    BF_PUBLICATION("publication", Ld4lProperty.HAS_PROVISION),
    BF_RELATED_INSTANCE("relatedInstance", Ld4lProperty.RELATED),
    BF_RELATED_WORK("relatedWork", Ld4lProperty.RELATED),    
    BF_RELATOR("relator", Ld4lProperty.HAS_CONTRIBUTION),
    BF_REPRODUCTION("reproduction"),
    BF_RESOURCE_PART("resourcePart"),
    BF_REVIEW("review", Ld4lProperty.HAS_BODY),
    BF_REVIEW_OF("reviewOf", Ld4lProperty.HAS_TARGET),
    BF_SHELF_MARK("shelfMark", Ld4lProperty.HAS_SHELF_MARK),
    BF_SHELF_MARK_DDC("shelfMarkDdc", Ld4lProperty.HAS_SHELF_MARK),
    BF_SHELF_MARK_LCC("shelfMarkLcc", Ld4lProperty.HAS_SHELF_MARK),
    BF_SHELF_MARK_NLM("shelfMarkNlm", Ld4lProperty.HAS_SHELF_MARK),
    BF_SHELF_MARK_SCHEME("shelfMarkScheme"),
    BF_SHELF_MARK_UDC("shelfMarkUdc", Ld4lProperty.HAS_SHELF_MARK),
    BF_SUBJECT("subject", Ld4lProperty.HAS_SUBJECT),
    BF_SUBTITLE("subtitle"),
    BF_SUCCEEDED_BY("succeededBy", Ld4lProperty.PRECEDES),    
    BF_SUMMARY("review", Ld4lProperty.HAS_BODY),
    BF_SUMMARY_OF("reviewOf", Ld4lProperty.HAS_TARGET),
    BF_SUPPLEMENTARY_CONTENT_NOTE("supplementaryContentNote", 
            Ld4lProperty.LEGACY_SUPPLEMENTARY_CONTENT_NOTE),
    BF_SYSTEM_NUMBER("systemNumber", Ld4lProperty.IDENTIFIED_BY),
    BF_TITLE("title", Ld4lProperty.HAS_TITLE),
    BF_TITLE_STATEMENT("titleStatement"),
    BF_TITLE_VALUE("titleValue", Ld4lProperty.LABEL),
    BF_TRANSLATION("translation", Ld4lProperty.TRANSLATED_AS),
    BF_TRANSLATION_OF("translationOf", Ld4lProperty.TRANSLATES),
    BF_WORK_TITLE("workTitle", Ld4lProperty.HAS_TITLE),
       
    MADSRDF_AUTHORITATIVE_LABEL(OntNamespace.MADSRDF, "authoritativeLabel", 
            Ld4lProperty.MADSRDF_AUTHORITATIVE_LABEL),
    MADSRDF_IS_MEMBER_OF_MADS_SCHEME(OntNamespace.MADSRDF, 
            "isMemberOfMADSScheme", 
            Ld4lProperty.MADSRDF_IS_MEMBER_OF_MADS_SCHEME),

    // Add others as appropriate
    RELATORS_AUTHOR(
            OntNamespace.RELATORS, "aut", Ld4lProperty.HAS_CONTRIBUTION), 
    RELATORS_COMPOSER(
            OntNamespace.RELATORS, "cmp", Ld4lProperty.HAS_CONTRIBUTION),  
    RELATORS_CONDUCTOR(
            OntNamespace.RELATORS, "cnd", Ld4lProperty.HAS_CONTRIBUTION),  
    RELATORS_EDITOR(
            OntNamespace.RELATORS, "edt", Ld4lProperty.HAS_CONTRIBUTION),  
    RELATORS_NARRATOR(
            OntNamespace.RELATORS, "nrt", Ld4lProperty.HAS_CONTRIBUTION),  
    RELATORS_PERFORMER(
            OntNamespace.RELATORS, "prf", Ld4lProperty.HAS_CONTRIBUTION); 

    private final OntNamespace namespace;
    private final String localname;
    private final String uri;
    private final String prefixed;
    private final Property property;
    private final Ld4lProperty ld4lProperty;
    

    BfProperty(String localname) {
        // Assign default namespace for this enum value
        // No corresponding Ld4lProperty for this enum value
        this(OntNamespace.BIBFRAME, localname, null);
    }

    BfProperty(OntNamespace namespace, String localname) {
        // Assign default namespace to this enum value
        // No corresponding Ld4lProperty for this enum value
        this(namespace, localname, null);
    }
    
    BfProperty(String localname, Ld4lProperty ld4lProperty) {
        // Assign default namespace to this enum value
        this(OntNamespace.BIBFRAME, localname, ld4lProperty);
    }
    
    BfProperty(OntNamespace namespace, String localname, 
            Ld4lProperty ld4lProperty) {
        
        // Or use a Namespace?
        this.namespace = namespace;
        this.localname = localname;
        this.ld4lProperty = ld4lProperty;
        
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
    
    public Ld4lProperty ld4lProperty() {
        return ld4lProperty;
    }
    
    public static List<BfProperty> authorityProperties() {
        return Arrays.asList(
                BfProperty.BF_AUTHORITY_SOURCE,
                BfProperty.BF_HAS_AUTHORITY,
                BfProperty.BF_AUTHORIZED_ACCESS_POINT
                );        
    }
    
    private static final Map<Property, BfProperty> LOOKUP_BY_JENA_PROP = 
            new HashMap<Property, BfProperty>();
    private static final Map<Property, Property> PROPERTY_MAP = 
            new HashMap<Property, Property>();
    
    static {
        for (BfProperty bfProp : values()) {
            Property prop = bfProp.property;
            LOOKUP_BY_JENA_PROP.put(prop,  bfProp);
            if (bfProp.ld4lProperty != null) {
                PROPERTY_MAP.put(
                        bfProp.property(), bfProp.ld4lProperty.property());
            }
        }
    }
    
    public static BfProperty get(Property prop) {
        return LOOKUP_BY_JENA_PROP.get(prop);
    }
    
    public static Map<Property, Property> propertyMap() {
        return PROPERTY_MAP;
    }

    public static Map<Property, Property> propertyMap(
            Map<BfProperty, Ld4lProperty> map) {

        Map<Property, Property> propertyMap = 
                new HashMap<Property, Property>();
                   
        for (Map.Entry<BfProperty, Ld4lProperty> entry : map.entrySet()) {
            propertyMap.put(entry.getKey().property, 
                    entry.getValue().property());
        }
        
        return propertyMap;
    }
    
    public static List<Property> properties(List<BfProperty> bfProps) {

        List<Property> properties = new ArrayList<Property>();
        for (BfProperty bfProp : bfProps) {
            properties.add(bfProp.property);
        }
        
        return properties;
        
    }
   
}
