package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

public class BfAuthorityConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfAuthorityConverter.class);
       
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.NAME);
    }
    
    private static ParameterizedSparqlString RESOURCE_SUBMODEL_PSS = 
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
    
    
    public BfAuthorityConverter(String localNamespace) {
        super(localNamespace);
    }

    @Override
    protected ParameterizedSparqlString getResourceSubModelPss() {
        return RESOURCE_SUBMODEL_PSS;
    }
    
    @Override
    protected Map<Property, Property> getPropertyMap() {
        // WRONG - alters map returned by BfProperty.propertyMap()
        // Map<Property, Property> PROPERTY_MAP = BfProperty.propertyMap();      
        // PROPERTY_MAP.putAll(getPropertyMap());
        Map<Property, Property> map = new HashMap<Property, Property>();
        
        // Get default mapping from Bibframe to LD4L properties
        map.putAll(BfProperty.propertyMap());
        
        // These properties have a non-default conversion for bf:Authorities.
        map.putAll(
                BfProperty.propertyMap(PROPERTY_MAP));
        
        return map;        
    }
    
    protected void removeMadsAuthority() {
        Statement statement = 
                subject.getProperty(BfProperty.BF_HAS_AUTHORITY.property());
        Resource madsAuthority = statement.getResource();
        removeResource(madsAuthority);
    }

}
