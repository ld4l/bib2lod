package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MadsAuthorityConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(MadsAuthorityConverter.class);
    
    public MadsAuthorityConverter(String localNamespace) {
        super(localNamespace);
    }
    
    // TODO - add convert method - need to look at the subject of the
    // bf:hasAuthority statement. If a meeting, remove the authority,
    // and all associated stmts. 
    // remove the mads complex subject type everywhere
//    protected Model convert() {
//        LOGGER.debug("converting mads auth");
//        return super.convert();
//    }

}
