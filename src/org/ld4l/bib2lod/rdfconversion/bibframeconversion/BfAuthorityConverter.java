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
       
    private static final Map<BfProperty, Ld4lProperty> propertyMap = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        propertyMap.put(BfProperty.BF_LABEL, Ld4lProperty.NAME);
    }
    
    private static ParameterizedSparqlString resourceSubModelPss = 
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
        return resourceSubModelPss;
    }
    
    @Override
    protected Map<Property, Property> getPropertyMap() {
        // WRONG - alters map returned by BfProperty.propertyMap()
        // Map<Property, Property> propertyMap = BfProperty.propertyMap();      
        // propertyMap.putAll(getPropertyMap());
        Map<Property, Property> map = new HashMap<Property, Property>();
        
        // Get default mapping from Bibframe to LD4L properties
        map.putAll(BfProperty.propertyMap());
        
        // These properties have a non-default conversion for bf:Authorities.
        map.putAll(
                BfProperty.propertyMap(propertyMap));
        
        return map;        
    }


}
