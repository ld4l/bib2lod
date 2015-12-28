package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfTopicConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTopicConverter.class);
    
    private static final List<BfProperty> FAST_PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        FAST_PROPERTIES_TO_RETRACT.add(BfProperty.BF_HAS_AUTHORITY);
        FAST_PROPERTIES_TO_RETRACT.add(BfProperty.BF_SYSTEM_NUMBER);
    }
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.PREFERRED_LABEL);
    }

    public BfTopicConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }


    /* 
     * FAST topics don't need a system number, since the FAST URI replaces it,
     * so we remove it.
     * They are also not authorities, so we remove the MADS Authority from the
     * model. 
     * Note that Topics with local URIs retain these related resources.
     */    
    @Override
    protected List<Property> getResourcesToRemove() {
        String namespace = subject.getNameSpace();
        if (namespace.equals(Vocabulary.FAST.uri())) {
            return BfProperty.properties(FAST_PROPERTIES_TO_RETRACT);
        }
        return new ArrayList<Property>();        
    }
    
    @Override
    protected Map<Property, Property> getPropertyMap() {
        return BfProperty.propertyMap(PROPERTY_MAP);
    }
}
