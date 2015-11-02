package org.ld4l.bib2lod.rdfconversion;

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
    BF_AUTHORIZED_ACCESS_POINT("authorizedAccessPoint"),
    BF_BARCODE("barcode"),
    BF_EVENT_PLACE("eventPlace"),
    BF_HAS_ANNOTATION("hasAnnotation"),
    BF_HAS_AUTHORITY("hasAuthority"),
    BF_HOLDING_FOR("holdingFor"),
    BF_IDENTIFIER("identifier"),
    BF_IDENTIFIER_SCHEME("identifierScheme"),
    BF_IDENTIFIER_VALUE("identifierValue"),
    BF_ITEM_ID("itemId"),
    BF_LABEL("label"),
    BF_SHELF_MARK("shelfMark"),
    BF_SHELF_MARK_DDC("shelfMarkDdc"),
    BF_SHELF_MARK_LCC("shelfMarkLcc"),
    BF_SHELF_MARK_NLM("shelfMarkNlm"),
    BF_SHELF_MARK_SCHEME("shelfMarkScheme"),
    BF_SHELF_MARK_UDC("shelfMarkUdc"),
    BF_SYSTEM_NUMBER("systemNumber"),
       
    MADSRDF_AUTHORITATIVE_LABEL(OntNamespace.MADSRDF, "authoritativeLabel"),
 
    MADSRDF_IS_MEMBER_OF_MADS_SCHEME(
            OntNamespace.MADSRDF, "isMemberOfMADSScheme");

    private final OntNamespace namespace;
    private final String localname;
    private final String uri;
    private final String prefixed;
    private final Property property;
    

    BfProperty(String localname) {
        // Default namespace for this enum type
        this(OntNamespace.BIBFRAME, localname);
    }
    
    BfProperty(OntNamespace namespace, String localname) {
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
