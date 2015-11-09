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
    
    private static final Ld4lType NEW_TYPE = Ld4lType.MEETING;
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        // TODO Parse label into foaf:name, date, place; postponing due to
        // complexity.
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.LABEL);
        PROPERTY_MAP.put(BfProperty.BF_HAS_AUTHORITY, 
                Ld4lProperty.IDENTIFIED_BY_AUTHORITY);
        PROPERTY_MAP.put(BfProperty.BF_SYSTEM_NUMBER, 
                Ld4lProperty.IDENTIFIED_BY);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            Arrays.asList(
                    BfProperty.BF_AUTHORITY_SOURCE,
                    BfProperty.BF_AUTHORIZED_ACCESS_POINT
            );
            
    
    public BfMeetingConverter(Resource subject) {
        super(subject);
    }
    
    @Override
    protected Ld4lType getNewType() {
        return NEW_TYPE;
    }

    @Override
    protected Map<BfProperty, Ld4lProperty> getPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected List<BfProperty> getPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }

    @Override
    public Model convert() {  

        assignType();  
        // NB May move to BfResourceConverter
        convertSystemNumber();
        convertConferenceName();
        convertProperties();
        retractProperties(); 
        // Doesn't seem to be needed here
        changePropertyNamespaces();
        return subject.getModel();
    }
    
    private void convertSystemNumber() {
        
        Model model = subject.getModel();
        Property prop = BfProperty.BF_SYSTEM_NUMBER.property();
        
        Model assertions = ModelFactory.createDefaultModel();
        
        StmtIterator stmts = subject.listProperties(prop);
        while (stmts.hasNext()) {
            Statement statement = stmts.nextStatement();
            Resource object = statement.getResource();
            assertions.add(object, RDF.type, Ld4lType.IDENTIFIER.ontClass());
        }
        
        model.add(assertions);
        
    }
    
    private void convertConferenceName() {

        StmtIterator stmts = subject.getModel().listStatements(null, RDF.type, 
                BfType.MADSRDF_CONFERENCE_NAME.ontClass());
        if (stmts.hasNext()) { 
            subject.addProperty(RDF.type, Ld4lType.CONFERENCE.ontClass());
            // Not retracting the original statement or any other statements
            // about the madsrdf:Authority.
        }

    }
    

}
