package org.ld4l.bib2lod.rdfconversion.typededuping;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
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
        Map<String, String> uniqueAgents = new HashMap<String, String>();
        Map<String, String> uniqueAuths = new HashMap<String, String>();
        
        Query query = getQuery(type);        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String agentUri = soln.getResource("agent").getURI();
            String key = getAgentKey(soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.debug("No key for " + agentUri + "; can't dedupe");
                continue;
            }

            LOGGER.debug("Original uri: " + agentUri + " => " + key);
            key = normalizeAuthorityName(key);
            
            Resource auth = soln.getResource("auth");
            String authUri = auth == null ? null : auth.getURI();            

            if (uniqueAgents.containsKey(key)) {
                // We've seen this individual before
                String uniqueAgentUri = uniqueAgents.get(key);
                LOGGER.debug("Found matching value for key " + key 
                        + " and agent URI " + agentUri);
                LOGGER.debug("Adding: " + agentUri + " => " + uniqueAgentUri);
                uniqueUris.put(agentUri, uniqueAgentUri);
                // NB The algorithm assumes the agent key and the authority
                // key will always be identical. If not, we should normalize
                // auth labels and dedupe authorities on them, rather than on 
                // the agent keys.
                if (authUri != null) {
                    if (uniqueAuths.containsKey(key)) {
                        String uniqueAuthUri = uniqueAuths.get(key);
                        LOGGER.debug("Found matching value for key " 
                                    + key + " and auth URI " + authUri);
                        LOGGER.debug("Adding: " + authUri + " => " 
                                    + uniqueAuthUri);
                        uniqueUris.put(authUri, uniqueAuthUri);
                    } else {
                        LOGGER.debug("Didn't find auth URI for" + key);
                        uniqueAuths.put(key, authUri);
                        uniqueUris.put(authUri, authUri);
                    }
                }
            } else {
                // We haven't seen this individual before
                LOGGER.debug("New agent: " + agentUri);
                uniqueUris.put(agentUri, agentUri);
                uniqueAgents.put(key, agentUri);
                if (authUri != null) {
                    LOGGER.debug("New auth: " + authUri);
                    uniqueUris.put(authUri, authUri);                
                    uniqueAuths.put(key, authUri);
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
        
        String commandText = 
                /* NB We're not getting the authority label because we assume 
                 * it's identical to the agent label, so we can use that to 
                 * dedupe the authorities as well. If that turns out to be 
                 * wrong, we'll need to normalize the auth labels and dedupe 
                 * authorities against them directly.
                 */
                // "SELECT ?agent ?authAccessPt ?label ?auth ?authLabel "
                "SELECT ?agent ?authAccessPoint ?label ?auth "
                + "WHERE { "
                + "?agent a ?type . "
                + "OPTIONAL { ?agent "
                + OntologyProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() + " "
                + "?authAccessPoint . } "
                + "OPTIONAL { ?agent  " 
                + OntologyProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "OPTIONAL { ?agent " 
                + OntologyProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?auth . "
    //                + "?auth " 
    //                + OntologyProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
    //                + " ?authLabel . "
                + "} }";

        pss.setCommandText(commandText);
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        return pss.asQuery();            

    }
    
    private String getAgentKey(QuerySolution soln) {
        return getDefaultKey(soln);
    }
    

}
