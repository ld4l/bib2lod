package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfMeetingConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfMeetingConverter.class);

    private static List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_AUTHORITY_SOURCE);
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_AUTHORIZED_ACCESS_POINT);
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_HAS_AUTHORITY);
    }
    
    private final ParameterizedSparqlString ASK_PSS = 
            new ParameterizedSparqlString(
                    "ASK { "
                    + "?meeting " + BfProperty.BF_HAS_AUTHORITY.sparqlUri() 
                    + " " 
                    + "?madsAuth . "
                    + "?madsAuth a " 
                    + BfType.MADSRDF_CONFERENCE_NAME.sparqlUri()
                    + "}");
    
//    ParameterizedSparqlString selectPss = new ParameterizedSparqlString(
//            "SELECT ?idValue WHERE {"
//            + "?work + " + BfProperty.BF_SUBJECT.sparqlUri() + " "
//            + "?meeting . "
//            + "?meeting " + BfProperty.BF_SYSTEM_NUMBER.sparqlUri() + " "
//            + "?identifier . "
//            + "?identifier " + BfProperty.BF_IDENTIFIER_VALUE.sparqlUri() + " "
//            + "?idValue . "
//            + "}");

    
    public BfMeetingConverter(String localNamespace) {
        super(localNamespace);
    }
    
    @Override
    protected Model convert() {

        convertConferenceName();
        
        removeMadsAuthority();
        
        // Do with the identifier - will be common to other types
        // convertFastIdentifier();
        
        // if the meeting is a subject, and there's a fast identifier, 
        // create a sameas between the meeting and the fast uri

        return super.convert();        
    }
      
    private void convertConferenceName() {

        ASK_PSS.setIri("meeting", subject.getURI());
        Query query = ASK_PSS.asQuery();
        LOGGER.debug(query.toString());
        QueryExecution qexec = 
                QueryExecutionFactory.create(query, subject.getModel());
        Boolean isConfName = qexec.execAsk();
        if (isConfName) {
                outputModel.add(
                        subject, RDF.type, Ld4lType.CONFERENCE.type());
        }
        
        qexec.close();        
    }
    
    
    @Override
    protected Map<Property, Property> getPropertyMap() {
        // WRONG - alters map returned by BfProperty.propertyMap()
        // Map<Property, Property> propertyMap = BfProperty.propertyMap();      
        // propertyMap.putAll(getPropertyMap());
        Map<Property, Property> propertyMap = new HashMap<Property, Property>();
        
        // Get default mapping from Bibframe to LD4L properties
        propertyMap.putAll(BfProperty.propertyMap());
        
        // For Meetings, these properties are dropped rather than converted.
        propertyMap.keySet().removeAll(
                BfProperty.properties(PROPERTIES_TO_RETRACT));
        
        return propertyMap;
    }
    
    public List<Resource> getResourcesToRemove() {
        return resourcesToRemove;
    }
    
//    private void convertFastIdentifier() {
//        
//        selectPss.setIri("meeting", subject.getURI());
//        Query query = selectPss.asQuery();
//        QueryExecution qexec = 
//                QueryExecutionFactory.create(query, subject.getModel());
//        ResultSet results = qexec.execSelect();
//        while (results.hasNext()) {
//            
//        }
//        // get the fast uri  - static identifierConverter method
//        // if not null, add the same as to the output model
//        // add same as to fast subject
//    }
    
}
