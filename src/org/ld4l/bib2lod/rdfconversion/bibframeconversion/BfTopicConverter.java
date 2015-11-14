package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfTopicConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTopicConverter.class);
  
    private static final Map<Property, Property> PROPERTY_MAP =
            new HashMap<Property, Property>();
    static {
 
    }

    

}
