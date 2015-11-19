package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

public class BfTemporalConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTemporalConverter.class);
    

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL,Ld4lProperty.LABEL);              
    }

    public BfTemporalConverter(BfType bfType) {
        super(bfType);
    }
    

    
    @Override
    protected Map<BfProperty, Ld4lProperty> getBfPropertyMap() {
        Map<BfProperty, Ld4lProperty> propertyMap = super.getBfPropertyMap();
        // Converter-specific value overrides superclass value.
        propertyMap.putAll(PROPERTY_MAP);
        return propertyMap;
    }
    

}
