package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfFamilyConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfFamilyConverter.class);
    
    private static final Ld4lType NEW_TYPE = Ld4lType.FAMILY;
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.NAME);
        PROPERTY_MAP.put(BfProperty.BF_HAS_AUTHORITY, 
                Ld4lProperty.IDENTIFIED_BY_AUTHORITY);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            Arrays.asList(
                    BfProperty.BF_AUTHORIZED_ACCESS_POINT
            );
            
    
    public BfFamilyConverter(Resource subject) {
        super(subject);
    }
    
    @Override
    protected Ld4lType getNewType() {
        return NEW_TYPE;
    }

    @Override
    protected Map<BfProperty, Ld4lProperty> getPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected List<BfProperty> getPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }
    

}
