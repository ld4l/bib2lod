package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfTopicConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTopicConverter.class);

    private static ParameterizedSparqlString constructPss = 
            new ParameterizedSparqlString(
                    "CONSTRUCT { ?resource ?p1 ?o1 . "
                    + " ?o1 ?p2 ?o2 . "
                    + " ?s ?p3 ?resource . " 
                    + "} WHERE { { "  
                    + "?resource ?p1 ?o1 . "
                    + "OPTIONAL { ?o1 ?p2 ?o2 . } "
                    + "} UNION { " 
                    + "?s ?p3 ?resource . "
                    + "} } ");

   
    
    private static final Map<BfProperty, Ld4lProperty> FAST_PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        FAST_PROPERTY_MAP.put(
                BfProperty.BF_LABEL, Ld4lProperty.PREFERRED_LABEL);
    }

    public BfTopicConverter(String localNamespace) {
        super(localNamespace);
    }

    @Override
    protected Model convert() {
        
        if (isFastHeading()) {
            removeMadsAuthority();
            removeIdentifier();
        }
        
        return super.convert();
    }
    
    @Override
    protected Map<Property, Property> getPropertyMap() {
        
        if (isFastHeading()) {
            return BfProperty.propertyMap(FAST_PROPERTY_MAP);
        }

        return super.getPropertyMap();     
    }
    
    private boolean isFastHeading() {
        return subject.getNameSpace().equals(Vocabulary.FAST.uri());
    }

    protected void removeIdentifier() {
        Statement statement = 
                subject.getProperty(BfProperty.BF_SYSTEM_NUMBER.property());
        if (statement != null) {
            Resource identifier = statement.getResource();
            removeResource(identifier);
        }
    }
}
