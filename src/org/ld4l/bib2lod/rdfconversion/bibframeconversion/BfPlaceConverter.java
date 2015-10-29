package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfPlaceConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfPlaceConverter.class);
    
    public BfPlaceConverter(OntType type) {
        super(type);
    }
    
    public Model convert(Resource subject) {
        
        Model model = subject.getModel();
        // HERE**** Need a type for Place - check Rob's doc
        //subject.addProperty(RDF.type, createResource(OntType.))
        
        return model;
    }

}
