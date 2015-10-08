package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public abstract class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);
    
    public BfResourceConverter() {
        // TODO Auto-generated constructor stub
        LOGGER.info("In BfResourceConverter constructor");
    }

}
