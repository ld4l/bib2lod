package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfEventConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfEventConverter.class);
    
    private static final OntType NEW_TYPE = OntType.EVENT;
    
    private static final Map<OntProperty, OntProperty> PROPERTY_MAP = 
            new HashMap<OntProperty, OntProperty>();
    static {
        PROPERTY_MAP.put(OntProperty.BF_EVENT_PLACE, OntProperty.LOCATION);
    }


    public BfEventConverter(Resource subject) {
        super(subject);
    }
    
    @Override
    protected OntType getNewType() {
        return NEW_TYPE;
    }

    @Override
    protected Map<OntProperty, OntProperty> getPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected List<OntProperty> getPropertiesToRetract() {
        return null;
    }
    
}
