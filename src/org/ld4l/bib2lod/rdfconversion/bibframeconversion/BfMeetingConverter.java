package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfMeetingConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfMeetingConverter.class);
   
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        // TODO Parse label into foaf:name, date, place; postponing due to
        // complexity.
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.LABEL);
        PROPERTY_MAP.put(BfProperty.BF_SYSTEM_NUMBER, 
                Ld4lProperty.IDENTIFIED_BY);
    }
    

    @Override
    protected Map<BfProperty, Ld4lProperty> getPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected void convertProperties() {
        convertSystemNumber();
        convertConferenceName();
        super.convertProperties();        
    }
    
    private void convertSystemNumber() {
        
        Property prop = BfProperty.BF_SYSTEM_NUMBER.property();
        
        Model assertions = ModelFactory.createDefaultModel();
        
        StmtIterator stmts = subject.listProperties(prop);
        while (stmts.hasNext()) {
            Statement statement = stmts.nextStatement();
            Resource object = statement.getResource();
            assertions.add(object, RDF.type, Ld4lType.IDENTIFIER.ontClass());
        }
        
        subject.getModel().add(assertions);
        
    }
    
    private void convertConferenceName() {

        StmtIterator stmts = model.listStatements(null, RDF.type, 
                BfType.MADSRDF_CONFERENCE_NAME.ontClass());
        if (stmts.hasNext()) { 
            subject.addProperty(RDF.type, Ld4lType.CONFERENCE.ontClass());
            // Not retracting the original statement or any other statements
            // about the madsrdf:Authority.
        }

    }

}
