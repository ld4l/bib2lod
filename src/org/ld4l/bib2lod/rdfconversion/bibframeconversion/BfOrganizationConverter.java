package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfType;

public class BfOrganizationConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfOrganizationConverter.class);
    

    public BfOrganizationConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }

}
