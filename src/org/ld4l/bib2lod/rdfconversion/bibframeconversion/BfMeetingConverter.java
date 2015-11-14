package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
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
   
    
    private static final Map<Property, Property> PROPERTY_MAP =
            new HashMap<Property, Property>();
    static {
        // TODO Parse label into foaf:name, date, place; postponing due to
        // complexity.
        PROPERTY_MAP.put(BfProperty.BF_LABEL.property(), 
                Ld4lProperty.LABEL.property());
        PROPERTY_MAP.put(BfProperty.BF_SYSTEM_NUMBER.property(), 
                Ld4lProperty.IDENTIFIED_BY.property());
    }
    

    @Override
    protected Map<Property, Property> getPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected void convert() {
        convertSystemNumber();
        convertConferenceName();
        super.convert();        
    }
    
    private void convertSystemNumber() {
        
        Property prop = BfProperty.BF_SYSTEM_NUMBER.property();
              
        StmtIterator stmts = subject.listProperties(prop);
        while (stmts.hasNext()) {
            Statement statement = stmts.nextStatement();
            Resource object = statement.getResource();
            // Add to assertions model rather than main model, so the
            // statement doesn't get reprocessed.
            assertions.add(object, RDF.type, Ld4lType.IDENTIFIER.ontClass());
        }        
    }
    
    private void convertConferenceName() {

        StmtIterator stmts = model.listStatements(null, RDF.type, 
                BfType.MADSRDF_CONFERENCE_NAME.ontClass());
        if (stmts.hasNext()) { 
            // Add to assertions model rather than main model, so the
            // statement doesn't get reprocessed.
            assertions.add(subject, RDF.type, Ld4lType.CONFERENCE.ontClass());
            // Not retracting the original statement or any other statements
            // about the madsrdf:Authority.
        }

    }

}
