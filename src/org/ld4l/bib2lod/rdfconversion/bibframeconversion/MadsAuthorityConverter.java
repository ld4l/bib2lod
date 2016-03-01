package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MadsAuthorityConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(MadsAuthorityConverter.class);
    
    public MadsAuthorityConverter(String localNamespace) {
        super(localNamespace);
    }    

}
