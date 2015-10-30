package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfPlaceConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfPlaceConverter.class);
    
    private static final OntType NEW_TYPE = OntType.PLACE;
    
    public BfPlaceConverter(Resource subject) {
        super(subject);
    }
    
    @Override
    protected OntType getNewType() {
        return NEW_TYPE;
    }

    @Override
    protected Map<OntProperty, OntProperty> getPropertyMap() {
        return null;
    }
    
    @Override
    protected List<OntProperty> getPropertiesToRetract() {
        return null;
    }

}
