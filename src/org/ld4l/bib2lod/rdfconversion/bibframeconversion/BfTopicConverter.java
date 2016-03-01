package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

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

    
    private static final List<BfProperty> FAST_PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        FAST_PROPERTIES_TO_RETRACT.add(BfProperty.BF_HAS_AUTHORITY);
       // FAST_PROPERTIES_TO_RETRACT.add(BfProperty.BF_SYSTEM_NUMBER);
    }
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.PREFERRED_LABEL);
    }

    public BfTopicConverter(String localNamespace) {
        super(localNamespace);
    }

    /* 
     * FAST topics don't need a system number, since the FAST URI replaces it,
     * so we remove it.
     * They are also not authorities, so we remove the MADS Authority from the
     * model. 
     * Note that Topics with local URIs retain these related resources.
     */   
    // MUST NOT WORK! This method is never called
//    @Override
//    protected List<Property> getResourcesToRemove() {
//        String namespace = subject.getNameSpace();
//        if (namespace.equals(Vocabulary.FAST.uri())) {
//            return BfProperty.properties(FAST_PROPERTIES_TO_RETRACT);
//        }
//        return new ArrayList<Property>();        
//    }
    
    @Override
    protected Map<Property, Property> getPropertyMap() {
        // WRONG - alters map returned by BfProperty.propertyMap()
        // Map<Property, Property> propertyMap = BfProperty.propertyMap();      
        // propertyMap.putAll(getPropertyMap());
        Map<Property, Property> map = new HashMap<Property, Property>();
        
        // Get default mapping from Bibframe to LD4L properties
        map.putAll(BfProperty.propertyMap());
        
        // For Topics, these properties have a non-default conversion.
        map.putAll(BfProperty.propertyMap(PROPERTY_MAP));

        // And these properties are removed rather than converted.
        map.keySet().removeAll(
                BfProperty.properties(FAST_PROPERTIES_TO_RETRACT));
        
        return map;        
    }

}
