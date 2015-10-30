package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfHeldItemConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfHeldItemConverter.class);
    
    private static final Ld4lType NEW_TYPE = Ld4lType.ITEM;
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_HOLDING_FOR, Ld4lProperty.HOLDING_FOR);
    }

    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            Arrays.asList(
                    BfProperty.BF_LABEL
            );
            
    
    public BfHeldItemConverter(Resource subject) {
        super(subject);
    }
    
    @Override 
    public Model convert() {
        assignType(); 
        convertProperties();
        retractProperties();
        return subject.getModel();
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
