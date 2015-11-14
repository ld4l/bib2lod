package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

public class BfTemporalConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTemporalConverter.class);
    
    
    private static final Map<Property, Property> PROPERTY_MAP =
            new HashMap<Property, Property>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL.property(), 
                Ld4lProperty.LABEL.property());
    }


    @Override
    protected Map<Property, Property> getPropertyMap() {
        return PROPERTY_MAP;
    }
    

}
