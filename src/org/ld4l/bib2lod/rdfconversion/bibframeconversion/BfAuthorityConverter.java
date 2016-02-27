package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Property;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

public class BfAuthorityConverter extends ResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfAuthorityConverter.class);
       
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.NAME);
    }
    
    private static ParameterizedSparqlString resourceSubModelPss = 
            new ParameterizedSparqlString(
                    "CONSTRUCT { ?resource ?p1 ?o . "
                    + " ?s ?p2 ?resource . "  
                    + " ?o " 
                    + BfProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() + " "            
                    + "?authLabel . "
                    + "} WHERE {  { "                                                      
                    + "?resource ?p1 ?o . "
                    + " OPTIONAL { "
                    + "?o a " + BfType.MADSRDF_AUTHORITY.sparqlUri() + " . "
                    + "?o "
                    + BfProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() + " "                                  
                    + "?authLabel . } "
                    + "} UNION { "
                    + "?s ?p2 ?resource . "
                    + "} } ");
    

    public BfAuthorityConverter(String localNamespace) {
        super(localNamespace);
    }
    
    @Override
    protected Map<Property, Property> getPropertyMap() {
        return BfProperty.propertyMap(PROPERTY_MAP);
    }

}
