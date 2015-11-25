package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfType;

public class BfFamilyConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfFamilyConverter.class);
    
    public BfFamilyConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
}
