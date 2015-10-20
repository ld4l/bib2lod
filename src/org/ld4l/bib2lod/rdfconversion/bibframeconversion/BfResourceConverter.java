package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);
    
    protected OntType type;
    
    public BfResourceConverter(OntType type) {
        LOGGER.debug("In constructor for " + this.getClass().getName());
        this.type = type;
    }
    
    public Model convert(Model inputModel) {  
        return inputModel;
    }

    


}
