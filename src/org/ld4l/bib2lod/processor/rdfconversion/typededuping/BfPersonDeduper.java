package org.ld4l.bib2lod.processor.rdfconversion.typededuping;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BfPersonDeduper extends TypeDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfPersonDeduper.class);
    
    @Override
    // TODO Maybe pass the model in the constructor so it's an instance 
    // variable? See if we need that.
    public Map<String, String> dedupe(Model model) {
        
        LOGGER.trace("Deduping model");
        Map<String, String> uniqueUris = new HashMap<String, String>();
        return uniqueUris;        
    }

}
