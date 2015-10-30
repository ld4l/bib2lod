package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.List;

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
    
    public BfPlaceConverter(OntType type, Resource subject) {
        super(type, subject);
    }
    
    @Override
    public Model convert() {
        
        Model model = subject.getModel();
        
        subject.removeAll(RDF.type);
        addType(OntType.PLACE);

        return model;
    }

    @Override
    protected List<OntProperty> getPropertiesToRetract() {
        return null;
    }

}
