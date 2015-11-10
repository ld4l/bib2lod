package org.ld4l.bib2lod.rdfconversion;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;


/**
 * Define properties used by LD4L in the RDF output of the conversion process.
 * They may be in the LD4L namespace (the default) or in an external namespace.
 * The enum names don't indicate ontology (e.g., not FOAF_NAME, etc.) in 
 * order to abstract away from the particular ontologies and properties used. 
 * If they change, it only requires a change here rather than in enum 
 * references.
 */
public enum Ld4lProperty {

    BIRTHDATE(OntNamespace.SCHEMA, "birthDate"),
    DEATHDATE(OntNamespace.SCHEMA, "deathDate"),
    HAS_SHELF_MARK("hasShelfMark"), // or hasLocator
    HAS_LOCATION(OntNamespace.PROV, "atLocation"),
    IDENTIFIED_BY("identifiedBy"),
    IDENTIFIED_BY_AUTHORITY(OntNamespace.MADSRDF, "isIdentifiedByAuthority"),           
    IDENTIFIES_RWO(OntNamespace.MADSRDF, "identifiesRWO"),
    IS_HOLDING_FOR("isHoldingFor"),
    LABEL(OntNamespace.RDFS, "label"),
    LANGUAGE(OntNamespace.DCTERMS, "language"),
    NAME(OntNamespace.FOAF, "name"),
    ORIGINAL_LANGUAGE("hasOriginalLanguage"),
    SUBJECT(OntNamespace.DCTERMS, "subject");
    

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
