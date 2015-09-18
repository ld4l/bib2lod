package org.ld4l.bib2lod.rdfconversion.typededuping;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntologyProperty;
import org.ld4l.bib2lod.rdfconversion.OntologyType;

public class BfPersonDeduper extends TypeDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfPersonDeduper.class);
    private static final Query QUERY = QueryFactory.create(
            // NB We're not getting the authority label because we assume it's
            // identical to the person label, so we can use it to dedupe the
            // authorities as well. If that turns out to be wrong, we'll need
            // to normalize the auth labels and dedupe authorities against them
            // directly.
            // "SELECT ?person ?personLabel ?auth ?authLabel "
            "SELECT ?person ?personLabel ?auth "
            + "WHERE { "
            + "?person a " + OntologyType.BF_PERSON.sparqlUri() +  "; "
            + OntologyProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() 
            + "?personLabel . "
            + "OPTIONAL { ?person " 
            + OntologyProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?auth . "
//            + "?auth " 
//            + OntologyProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
//            + " ?authLabel . "
            + "} }"
    );


    @Override
    public Map<String, String> dedupe(Model model) {

        Map<String, String> uniqueUris = new HashMap<String, String>();
        Map<String, String> labelToPerson = new HashMap<String, String>();
        Map<String, String> labelToAuth = new HashMap<String, String>();
        
        QueryExecution qexec = QueryExecutionFactory.create(QUERY, model);
        ResultSet results = qexec.execSelect();
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String personUri = soln.getResource("person").getURI();
            String personLabel = soln.getLiteral("personLabel").getLexicalForm();
            Resource auth = soln.getResource("auth");
            String authUri = null;
            if (auth != null) {
                authUri = auth.getURI();
            }
            LOGGER.debug("Original uri: " + personUri + " => " + personLabel);
            personLabel = normalizeAuthorityName(personLabel);

            if (labelToPerson.containsKey(personLabel)) {
                // We've seen this person before
                String uniquePersonUri = labelToPerson.get(personLabel);
                LOGGER.debug("Found matching value for label " + personLabel + " and person URI " + personUri);
                LOGGER.debug("Adding: " + personUri + " => " + uniquePersonUri);
                uniqueUris.put(personUri, uniquePersonUri);
                // NB The algorithm assumes the person label and the authority
                // label will always be identical. If not, we should normalize
                // auth labels and dedupe authorities on them, rather than on 
                // the person labels.
                if (authUri != null) {
                    if (labelToAuth.containsKey(personLabel)) {
                        String uniqueAuthUri = labelToAuth.get(personLabel);
                        LOGGER.debug("Found matching value for label " + personLabel + " and auth URI " + authUri);
                        LOGGER.debug("Adding: " + authUri + " => " + uniqueAuthUri);
                        uniqueUris.put(authUri, uniqueAuthUri);
                    } else {
                        LOGGER.debug("Didn't find auth URI for" + personLabel);
                        labelToAuth.put(personLabel, authUri);
                        uniqueUris.put(authUri, authUri);
                    }
                }
            } else {
                // We haven't seen this person before
                LOGGER.debug("New person: " + personUri);
                uniqueUris.put(personUri, personUri);
                labelToPerson.put(personLabel, personUri);
                if (authUri != null) {
                    LOGGER.debug("New auth: " + authUri);
                    uniqueUris.put(authUri, authUri);                
                    labelToAuth.put(personLabel, authUri);
                }
            }
        }
        if (LOGGER.isDebugEnabled()) {
            for (String uri : uniqueUris.keySet()) {
                LOGGER.debug(uri + " => " + uniqueUris.get(uri));
            }
        }
        return uniqueUris;        
    }

}
