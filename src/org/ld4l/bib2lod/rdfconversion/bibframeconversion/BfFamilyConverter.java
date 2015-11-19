package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

public class BfFamilyConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfFamilyConverter.class);
    
    public BfFamilyConverter(BfType bfType) {
        super(bfType);
    }
    
}
