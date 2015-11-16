package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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


    private static final List<BfType> TYPES_TO_CONVERT = 
            new ArrayList<BfType>();
    static {
        TYPES_TO_CONVERT.add(BfType.BF_MEETING);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {

    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.NAME);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {

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

    @Override 
    protected List<BfType> getBfTypesToConvert() {
        return TYPES_TO_CONVERT;
    }
    
    @Override
    protected List<BfProperty> getBfPropertiesToConvert() {
        return getBfAuthPropertiesToConvert();
    }
    
    @Override
    protected Map<BfProperty, Ld4lProperty> getBfPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected List<BfProperty> getBfPropertiesToRetract() {
        return getBfAuthPropertiesToRetract();
    }

}
