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

    BF_ANNOTATES("annotates"),
    BF_AUTHORITY_SOURCE("authoritySource"),
    BF_AUTHORIZED_ACCESS_POINT("authorizedAccessPoint"),
    BF_BARCODE("barcode"),
    BF_EVENT_PLACE("eventPlace", Ld4lProperty.HAS_LOCATION),
    BF_HAS_ANNOTATION("hasAnnotation", Ld4lProperty.HAS_ANNOTATION),
    BF_HAS_AUTHORITY("hasAuthority", Ld4lProperty.IDENTIFIED_BY_AUTHORITY),
    BF_HOLDING_FOR("holdingFor", Ld4lProperty.IS_HOLDING_FOR),
    BF_IDENTIFIER("identifier"),
    BF_IDENTIFIER_SCHEME("identifierScheme"),
    BF_IDENTIFIER_VALUE("identifierValue", Ld4lProperty.VALUE),
    BF_INSTANCE_OF("instanceOf", Ld4lProperty.IS_INSTANCE_OF),
    BF_ITEM_ID("itemId"),
    BF_LABEL("label", Ld4lProperty.LABEL),
    BF_LANGUAGE("language", Ld4lProperty.HAS_LANGUAGE),
    BF_LANGUAGE_OF_PART("languageOfPart", Ld4lProperty.LABEL), 
    BF_LANGUAGE_OF_PART_URI("languageOfPartUri"),
    BF_RESOURCE_PART("resourcePart"),
    BF_SHELF_MARK("shelfMark"),
    BF_SHELF_MARK_DDC("shelfMarkDdc"),
    BF_SHELF_MARK_LCC("shelfMarkLcc"),
    BF_SHELF_MARK_NLM("shelfMarkNlm"),
    BF_SHELF_MARK_SCHEME("shelfMarkScheme"),
    BF_SHELF_MARK_UDC("shelfMarkUdc"),
    BF_SUBJECT("subject", Ld4lProperty.HAS_SUBJECT),
    BF_SYSTEM_NUMBER("systemNumber", Ld4lProperty.IDENTIFIED_BY),
       
    MADSRDF_AUTHORITATIVE_LABEL(OntNamespace.MADSRDF, "authoritativeLabel"),
    MADSRDF_IS_MEMBER_OF_MADS_SCHEME(
            OntNamespace.MADSRDF, "isMemberOfMADSScheme");

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
    
//    private static final Map<Property, Property> PROPERTY_MAP = 
//            new HashMap<Property, Property>();
//    
//    static {
//        for (BfProperty bfProp : BfProperty.values()) {
//            if (bfProp.ld4lProperty != null) {
//                PROPERTY_MAP.put(bfProp.property, 
//                        bfProp.ld4lProperty.property());
//            }
//        }
//    }
//    
//    public static Map<Property, Property> propertyMap() {
//        return PROPERTY_MAP;
//    }
    
    public static Map<Property, Property> propertyMap(
            List<BfProperty> bfPropList,
            Map<BfProperty, Ld4lProperty> bfPropertyMap) {
        
        Map<Property, Property> propertyMap = new HashMap<Property, Property>();
        
        // These properties use the default mappings defined by each type
        propertyMap.putAll(propertyMap(bfPropList));
        
        // These properties use non-default mappings
        propertyMap.putAll(propertyMap(bfPropertyMap));
        
        return propertyMap;

    }
    
    public static Map<Property, Property> propertyMap(
            Map<BfProperty, Ld4lProperty> map) {

        Map<Property, Property> propertyMap = 
                new HashMap<Property, Property>();
        
        if (map != null) {            
            for (Map.Entry<BfProperty, Ld4lProperty> entry : map.entrySet()) {
                propertyMap.put(entry.getKey().property, 
                        entry.getValue().property());
            }
        }
        
        return propertyMap;
    }
    
    public static Map<Property, Property> propertyMap(
            List<BfProperty> bfProps) {
        
        Map<Property, Property> propertyMap = 
                new HashMap<Property, Property>();
        
        if (bfProps != null) {
            
            for (BfProperty bfProp : bfProps) {
                propertyMap.put(bfProp.property, 
                        bfProp.ld4lProperty.property());
            }
        }
        
        return propertyMap;
    }
    
    public static List<Property> propertyList(List<BfProperty> bfProps) {
        
        List<Property> props = new ArrayList<Property>();
        
        if (bfProps != null) {
            for (BfProperty bfProp : bfProps) {
                props.add(bfProp.property);
            }
        }
        return props;
    }
    
    public static List<BfProperty> authorityProperties() {
        return Arrays.asList(
                BfProperty.BF_AUTHORITY_SOURCE,
                BfProperty.BF_HAS_AUTHORITY,
                BfProperty.BF_AUTHORIZED_ACCESS_POINT
                );        
    }
    
}
