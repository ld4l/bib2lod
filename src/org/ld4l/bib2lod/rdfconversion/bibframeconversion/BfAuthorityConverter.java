package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

public class BfAuthorityConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfPlaceConverter.class);
    
    
    private static final List<BfType> TYPES_TO_CONVERT = 
            new ArrayList<BfType>();
    static {
        TYPES_TO_CONVERT.add(BfType.BF_IDENTIFIER);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_HAS_AUTHORITY);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_IDENTIFIER_VALUE);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_SYSTEM_NUMBER);
    }
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.NAME);
    }

    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_AUTHORITY_SOURCE);
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_AUTHORIZED_ACCESS_POINT);
    }

    public BfAuthorityConverter(BfType bfType) {
        super(bfType);
    }
    
    @Override
    protected List<BfType> getBfTypesToConvert() {
        List <BfType> typesToConvert = super.getBfTypesToConvert();
        typesToConvert.addAll(TYPES_TO_CONVERT);
        TYPES_TO_CONVERT.addAll(super.getBfTypesToConvert());
        return typesToConvert;
    }
    
    @Override
    protected List<BfProperty> getBfPropertiesToConvert() {
        return PROPERTIES_TO_CONVERT;
    }
    
    @Override
    protected Map<BfProperty, Ld4lProperty> getBfPropertyMap() {
        return PROPERTY_MAP;
    }
    
    @Override
    protected List<BfProperty> getBfPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }

}
