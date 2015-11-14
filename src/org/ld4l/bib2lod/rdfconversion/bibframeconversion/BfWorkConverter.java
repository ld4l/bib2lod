package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfWorkConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfWorkConverter.class);
    
    private static final Map<Property, Property> PROPERTY_MAP =
            new HashMap<Property, Property>();
    static {

    }

    

    @Override
    protected Map<Property, Property> getPropertyMap() {
        return PROPERTY_MAP;
    }

    
}
