package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfMeetingConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfMeetingConverter.class);

    private static List<BfProperty> propertiesToRetract = 
            new ArrayList<BfProperty>();
    static {
        propertiesToRetract.add(BfProperty.BF_AUTHORITY_SOURCE);
        propertiesToRetract.add(BfProperty.BF_AUTHORIZED_ACCESS_POINT);
        propertiesToRetract.add(BfProperty.BF_HAS_AUTHORITY);
    }
    
    private static ParameterizedSparqlString resourceSubModelPss = 
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
    
    ParameterizedSparqlString askPss = new ParameterizedSparqlString(
            "ASK { "
            + "?meeting " + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " " 
            + "?madsAuth . "
            + "?madsAuth a " + BfType.MADSRDF_CONFERENCE_NAME.sparqlUri()
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
    protected ParameterizedSparqlString getResourceSubModelPss() {
        return resourceSubModelPss;
    }
    
    
    @Override
    protected Model convert() {
        convertConferenceName();
        
        // Do with the identifier - will be common to other types
        // convertFastIdentifier();
        
        // if the meeting is a subject, and there's a fast identifier, 
        // create a sameas between the meeting and the fast uri

        return super.convert();        
    }
      
    private void convertConferenceName() {

        askPss.setIri("meeting", subject.getURI());
        Query query = askPss.asQuery();
        LOGGER.debug(query.toString());
        QueryExecution qexec = 
                QueryExecutionFactory.create(query, subject.getModel());
        Boolean isConfName = qexec.execAsk();
        if (isConfName) {
                outputModel.add(
                        subject, RDF.type, Ld4lType.CONFERENCE.ontClass());
        }
        
        qexec.close();        
    }
    
    
    @Override
    protected List<Property> getPropertiesToRetract() {
        return BfProperty.properties(propertiesToRetract);
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
