package org.ld4l.bib2lod.rdfconversion.typededuping;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntologyProperty;
import org.ld4l.bib2lod.rdfconversion.OntologyType;

public class BfAgentDeduper extends TypeDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfAgentDeduper.class);

 
    @Override
    public Map<String, String> dedupe(OntologyType type, Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());

        Map<String, String> uniqueUris = new HashMap<String, String>();
        Map<String, String> labelToAgent = new HashMap<String, String>();
        Map<String, String> labelToAuth = new HashMap<String, String>();
        
        Query query = getQuery(type);
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String agentUri = soln.getResource("agent").getURI();
            String agentLabel = soln.getLiteral("agentLabel").getLexicalForm();
            Resource auth = soln.getResource("auth");
            String authUri = null;
            if (auth != null) {
                authUri = auth.getURI();
            }
            LOGGER.debug("Original uri: " + agentUri + " => " + agentLabel);
            agentLabel = normalizeAuthorityName(agentLabel);

            if (labelToAgent.containsKey(agentLabel)) {
                // We've seen this agent before
                String uniqueAgentUri = labelToAgent.get(agentLabel);
                LOGGER.debug("Found matching value for label " + agentLabel 
                        + " and agent URI " + agentUri);
                LOGGER.debug("Adding: " + agentUri + " => " + uniqueAgentUri);
                uniqueUris.put(agentUri, uniqueAgentUri);
                // NB The algorithm assumes the agent label and the authority
                // label will always be identical. If not, we should normalize
                // auth labels and dedupe authorities on them, rather than on 
                // the agent labels.
                if (authUri != null) {
                    if (labelToAuth.containsKey(agentLabel)) {
                        String uniqueAuthUri = labelToAuth.get(agentLabel);
                        LOGGER.debug("Found matching value for label " 
                                    + agentLabel + " and auth URI " + authUri);
                        LOGGER.debug("Adding: " + authUri + " => " 
                                    + uniqueAuthUri);
                        uniqueUris.put(authUri, uniqueAuthUri);
                    } else {
                        LOGGER.debug("Didn't find auth URI for" + agentLabel);
                        labelToAuth.put(agentLabel, authUri);
                        uniqueUris.put(authUri, authUri);
                    }
                }
            } else {
                // We haven't seen this agent before
                LOGGER.debug("New agent: " + agentUri);
                uniqueUris.put(agentUri, agentUri);
                labelToAgent.put(agentLabel, agentUri);
                if (authUri != null) {
                    LOGGER.debug("New auth: " + authUri);
                    uniqueUris.put(authUri, authUri);                
                    labelToAuth.put(agentLabel, authUri);
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
    
    private Query getQuery(OntologyType type) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();

        pss.setCommandText(
            // NB We're not getting the authority label because we assume it's
            // identical to the agent label, so we can use it to dedupe the
            // authorities as well. If that turns out to be wrong, we'll need
            // to normalize the auth labels and dedupe authorities against them
            // directly.
            // "SELECT ?agent ?agentLabel ?auth ?authLabel "
            "SELECT ?agent ?agentLabel ?auth "
            + "WHERE { "
            + "?agent a ?type; "
            + OntologyProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() 
            + "?agentLabel . "
            + "OPTIONAL { ?agent " 
            + OntologyProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?auth . "
//                + "?auth " 
//                + OntologyProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
//                + " ?authLabel . "
            + "} }"
        );    
        
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        return pss.asQuery();
        
    }

}
