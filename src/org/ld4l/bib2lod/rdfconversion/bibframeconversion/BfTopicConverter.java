package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfTopicConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTopicConverter.class);

    private static ParameterizedSparqlString constructPss = 
            new ParameterizedSparqlString(
                    "CONSTRUCT { ?resource ?p1 ?o1 . "
                    + " ?o1 ?p2 ?o2 . "
                    + " ?s ?p3 ?resource . " 
                    + "} WHERE { { "  
                    + "?resource ?p1 ?o1 . "
                    + "OPTIONAL { ?o1 ?p2 ?o2 . } "
                    + "} UNION { " 
                    + "?s ?p3 ?resource . "
                    + "} } ");

   
    
    private static final Map<BfProperty, Ld4lProperty> FAST_PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        FAST_PROPERTY_MAP.put(
                BfProperty.BF_LABEL, Ld4lProperty.PREFERRED_LABEL);
    }
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.PREFERRED_LABEL);
    }

    public BfTopicConverter(String localNamespace) {
        super(localNamespace);
    }

    @Override
    protected Model convert() {
        
        if (isFastHeading()) {
            removeMadsAuthority();
            removeIdentifier();
        }
        
        return super.convert();
    }
    
    @Override
    protected Map<Property, Property> getPropertyMap() {
        
        if (isFastHeading()) {
            return BfProperty.propertyMap(FAST_PROPERTY_MAP);
        }

        // WRONG - alters map returned by BfProperty.propertyMap()
        // Map<Property, Property> PROPERTY_MAP = BfProperty.propertyMap();      
        // PROPERTY_MAP.putAll(getPropertyMap());
        Map<Property, Property> map = new HashMap<Property, Property>();
        
        // Get default mapping from Bibframe to LD4L properties
        map.putAll(BfProperty.propertyMap());
        
        // These properties have a conversion for Topics different from the
        // superclass (BfAuthorityConverter).
        map.putAll(
                BfProperty.propertyMap(PROPERTY_MAP));
        
        return map;          
    }
    
    @Override 
    protected Map<Resource, Resource> getTypeMap() {
        
        if (isFastHeading()) {
            // Return empty map - no type assertions for FAST topics
            return new HashMap<Resource, Resource>();
        }
        
        return super.getTypeMap();
    }
    
    private boolean isFastHeading() {
        return subject.getNameSpace().equals(Vocabulary.FAST.uri());
    }

    protected void removeIdentifier() {
        Statement statement = 
                subject.getProperty(BfProperty.BF_SYSTEM_NUMBER.property());
        if (statement != null) {
            Resource identifier = statement.getResource();
            removeResource(identifier);
        }
    }
}
