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
    
    public BfPlaceConverter(Resource subject) {
        super(subject);
    }
    
    @Override
    public Model convert() {

        subject.removeAll(RDF.type);
        addType(OntType.PLACE);

        return subject.getModel();
    }

    @Override
    protected List<OntProperty> getPropertiesToRetract() {
        return null;
    }

}
