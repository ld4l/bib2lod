package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfInstanceConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfInstanceConverter.class);
    
    private static final Ld4lType NEW_TYPE = Ld4lType.EVENT;
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        //PROPERTY_MAP.put(BfProperty.BF_EVENT_PLACE, Ld4lProperty.HAS_LOCATION);
    }

    
    @Override
    protected Ld4lType getNewType() {
        return null;
    }

    @Override
    protected Map<BfProperty, Ld4lProperty> getPropertyMap() {
        return null;
    }

    @Override
    protected List<BfProperty> getPropertiesToRetract() {
        return null;
    }
    
}
