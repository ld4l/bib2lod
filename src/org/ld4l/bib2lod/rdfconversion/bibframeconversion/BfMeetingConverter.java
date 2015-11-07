package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

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
                    BfProperty.BF_AUTHORIZED_ACCESS_POINT,
                    BfProperty.BF_AUTHORITY_SOURCE
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

/*
:meeting http://bibframe.org/vocab/authoritySource "http://id.loc.gov/vocabulary/subjectSchemes/fast"
:meeting http://bibframe.org/vocab/authorizedAccessPoint full-name-place-datetime-etc
:meeting http://bibframe.org/vocab/hasAuthority :madsAuth
:meeting http://bibframe.org/vocab/label same-as-authorized-access-point
:meeting http://bibframe.org/vocab/systemNumber 
:madsAuth http://www.w3.org/1999/02/22-rdf-syntax-ns#type mads:Authority
:madsAuth mads:authoritativeLabel same-as-label-etc
specific type: ConferenceName - keep
 
*/
        
        StmtIterator stmts = subject.getModel().listStatements();
        while (stmts.hasNext()) {
            Statement statement = stmts.nextStatement();
            LOGGER.debug(statement.toString());
        }
        assignType();  
        // NB May move to BfResourceConverter
        convertSystemNumber();
        convertConferenceName();
        convertProperties();
        retractProperties();    
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
        
        Model model = subject.getModel();

        StmtIterator stmts = model.listStatements(null, RDF.type, 
                BfType.MADSRDF_CONFERENCE_NAME.ontClass());
        if (stmts.hasNext()) { 
            subject.addProperty(RDF.type, Ld4lType.CONFERENCE.ontClass());
            // Not retracting the original statement or any other statements
            // about the madsrdf:Authority.
        }

    }
    

}
