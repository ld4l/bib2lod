package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class BfMeetingConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfMeetingConverter.class);


    ParameterizedSparqlString askPss = new ParameterizedSparqlString(
            "ASK { "
            + "?meeting " + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " " 
            + "?madsAuth . "
            + "?madsAuth a " + BfType.MADSRDF_CONFERENCE_NAME.sparqlUri()
            + "}");

    
    public BfMeetingConverter(String localNamespace) {
        super(localNamespace);
    }
    
    
    @Override
    protected Model convert() {
        RdfProcessor.printModel(subject.getModel(), "Meeting construct model:");
        convertConferenceName();
        
        /* 
         * TODO ** What to do with Identifier, Authority, since we're changing
         * Meeting from an Authority to an Event?
         * Predicates of which Meetings are subjects:
         * bf:authoritySource
         * bf:authorizedAccessPoint
         * bf:hasAuthority
         * bf:systemNumber
         * Predicates of which Meetings are objects:
         * bf:contributor
         * bf:creator
         * bf:subject
         */

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

}
