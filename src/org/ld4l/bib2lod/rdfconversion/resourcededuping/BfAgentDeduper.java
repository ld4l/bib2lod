package org.ld4l.bib2lod.rdfconversion.resourcededuping;

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
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;
import org.ld4l.bib2lod.rdfconversion.naco.NacoNormalizer;

public class BfAgentDeduper extends BfResourceDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfAgentDeduper.class);

    public BfAgentDeduper(OntType type) {
        super(type);
    }  
    
    @Override
    public Map<String, String> dedupe(Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());

        // Maps local URIs in the Bibframe RDF to deduped URIs (single, unique
        // URI per bf:Agent or madsrdf:Authority). This map will be used to
        // replace duplicate URIs for the same individual with a single, unique
        // URI.
        Map<String, String> uniqueUris = new HashMap<String, String>();
        
        // Maps keys for Agent identity matching to the unique Agent URIs. 
        Map<String, String> uniqueAgents = new HashMap<String, String>();
        
        // Maps keys for Authority identity matching to the unique Authority
        // URIs.
        Map<String, String> uniqueAuths = new HashMap<String, String>();

        // Execute the query
        Query query = getQuery(type);        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        // Loop through query results
        while (results.hasNext()) {
            
            QuerySolution soln = results.next();
            
            String agentUri = soln.getResource("agent").getURI();
            
            // Get key for agent identity matching.
            String key = getKey(soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.debug("No key for " + agentUri + "; can't dedupe");
                continue;
            }

            LOGGER.debug("Original uri: " + agentUri + " has key " + key);
    
            Resource auth = soln.getResource("auth");
            String authUri = auth == null ? null : auth.getURI();            

            // We've seen this Agent before
            if (uniqueAgents.containsKey(key)) {
                
                String uniqueAgentUri = uniqueAgents.get(key);
                LOGGER.debug("Found matching value for key " + key 
                        + " and agent URI " + agentUri);
                LOGGER.debug("Adding: " + agentUri + " => " + uniqueAgentUri);                
                // This local Agent URI will be replaced by the unique Agent URI
                // throughout the data
                uniqueUris.put(agentUri, uniqueAgentUri);
                
                // If there's an associated madsrdf:Authority, dedupe that
                // URI as well.
                // NB The algorithm assumes the agent key and the authority
                // key will always be identical, and uses the agent keys to 
                // dedupe authorities. If this assumption is incorrect, we need
                // to get an authority key and dedupe on that instead.
                if (authUri != null) {
                    
                    // We've seen this Authority before
                    if (uniqueAuths.containsKey(key)) {
                        String uniqueAuthUri = uniqueAuths.get(key);
                        LOGGER.debug("Found matching value for key " 
                                    + key + " and auth URI " + authUri);
                        LOGGER.debug("Adding: " + authUri + " => " 
                                    + uniqueAuthUri);
                        
                        // This local Authority URI will be replaced by the
                        // unique Authority URI throughout the data.
                        uniqueUris.put(authUri, uniqueAuthUri); 
                        
                    } else {
                        // We haven't seen this Authority before
                        LOGGER.debug("Didn't find auth URI for" + key);
                        // Add the local Authority URI to the maps
                        uniqueAuths.put(key, authUri);
                        uniqueUris.put(authUri, authUri);
                    }
                }
                
            } else {
                // We haven't seen this Agent before
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
            LOGGER.debug("uniqueUris map:");
            for (String uri : uniqueUris.keySet()) {
                LOGGER.debug(uri + " => " + uniqueUris.get(uri));
            }
            LOGGER.debug("uniqueAgents map:");
            for (String key : uniqueAgents.keySet()) {
                LOGGER.debug(key + " => " + uniqueAgents.get(key));
            }
        }
        
        return uniqueUris;        
    }
    
    protected Query getQuery(OntType type) {

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
                + OntProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() + " "
                + "?authAccessPoint . } "
                + "OPTIONAL { ?agent  " 
                + OntProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "OPTIONAL { ?agent " 
                + OntProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?auth . "
                // + "?auth " 
                // + OntologyProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
                // + " ?authLabel . "
                + "} }";

        pss.setCommandText(commandText);
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        return pss.asQuery();            

    }
    

}
