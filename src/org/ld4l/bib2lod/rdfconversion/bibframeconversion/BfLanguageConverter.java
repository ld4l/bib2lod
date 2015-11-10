package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

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

public class BfLanguageConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfLanguageConverter.class);
    
    private static final Ld4lType NEW_TYPE = Ld4lType.LANGUAGE;
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LANGUAGE, Ld4lProperty.LANGUAGE);
    }

//    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
//            Arrays.asList(
//                    BfProperty.BF_LANGUAGE_OF_PART_URI
//            );

    
    @Override
    protected void convert() {

        LOGGER.debug(model);
    }

    @Override
    protected void convertProperties() {
        convertResourcePart();
        super.convertProperties();
    }
    
    private void convertResourcePart() {
        
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
        return null; //PROPERTIES_TO_RETRACT;
    }
    
}
