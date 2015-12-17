package org.ld4l.bib2lod.rdfconversion.resourcededuping;

import java.time.Instant;
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
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.util.TimerUtils;

/**
 * Default deduper for bf:Authority subclasses. Use to dedupe bf:Authorities
 * unless a more specific deduper exists, such as BfTopicDeduper. 
 */
public class BfAuthorityDeduper extends BfResourceDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfAuthorityDeduper.class);
   

    public BfAuthorityDeduper(BfType type) {
        super(type);
    }  
    
    @Override
    public Map<String, String> dedupe(Model model) {
        
        LOGGER.trace("Deduping type " + type.toString());

        // Maps Authority URIs in the Bibframe RDF to deduped URIs (single, 
        // unique URI per bf:Authority or madsrdf:Authority). This map will be 
        // used to replace duplicate URIs for the same individual with a single, 
        // unique URI.
        Map<String, String> uniqueUris = new HashMap<String, String>();
        
        // Maps keys for local Authority identity matching to the unique 
        // local Authority URIs.
        Map<String, String> uniqueLocalAuths = new HashMap<String, String>();
        
        // Maps keys for external Authority identity matching to the unique 
        // external Authority URIs. NB These are external Authorities, which
        // seem to always be of type madsrdf:Authority, are linked to the 
        // bf:Authority via bf:hasAuthority.
        Map<String, String> uniqueExtAuths = new HashMap<String, String>();

        // Execute the query
        Query query = getQuery(type);        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        // Loop through query results
        int resourceCount = 0;
        Instant resourceStart = Instant.now();
        
        while (results.hasNext()) {
            
            resourceCount++;
            
            QuerySolution soln = results.next();
            
            String localAuthUri = soln.getResource("localAuth").getURI();
            
            // Get key for authority identity matching.
            String key = getKey(soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.trace("No key for " + localAuthUri + "; can't dedupe");
                continue;
            }

            LOGGER.trace("Original uri: " + localAuthUri + " has key " + key);
    
            Resource extAuth = soln.getResource("extAuth");
            String extAuthUri = extAuth == null ? null : extAuth.getURI();            

            // We've seen this Agent before
            if (uniqueLocalAuths.containsKey(key)) {
                
                String uniqueLocalAuthUri = uniqueLocalAuths.get(key);
                LOGGER.trace("Found matching value for key " + key 
                        + " and bf:Authority URI " + localAuthUri);
                LOGGER.trace("Adding: " + localAuthUri + " => " 
                        + uniqueLocalAuthUri);                
                // This local Authority URI will be replaced by the unique 
                // Authority URI throughout the data.
                uniqueUris.put(localAuthUri, uniqueLocalAuthUri);
                
                // If there's an associated external Authority, dedupe that
                // URI as well.
                // NB The algorithm assumes the local auth key and the external
                // auth key will always be identical, and uses the local auth
                // keys to dedupe external auths as well. If this assumption is
                // incorrect, we need to get key for the external auth from
                // madsrdf:authoritativeLabel and dedupe on that instead.
                if (extAuthUri != null) {
                    
                    // We've seen this Authority before
                    if (uniqueExtAuths.containsKey(key)) {
                        String uniqueAuthUri = uniqueExtAuths.get(key);
                        LOGGER.trace("Found matching value for key " + key 
                                    + " and external auth URI " + extAuthUri);
                        LOGGER.trace("Adding: " + extAuthUri + " => " 
                                    + uniqueAuthUri);
                        
                        // This local Authority URI will be replaced by the
                        // unique Authority URI throughout the data.
                        uniqueUris.put(extAuthUri, uniqueAuthUri); 
                        
                    } else {
                        // We haven't seen this Authority before
                        LOGGER.trace("Didn't find external auth URI for" + key);
                        // Add the local Authority URI to the maps
                        uniqueExtAuths.put(key, extAuthUri);
                        uniqueUris.put(extAuthUri, extAuthUri);
                    }
                }
                
            } else {
                // We haven't seen this local Authority before
                LOGGER.trace("New local auth: " + localAuthUri);
                // Not sure if this is needed in the map
                uniqueUris.put(localAuthUri, localAuthUri);
                uniqueLocalAuths.put(key, localAuthUri);
                if (extAuthUri != null) {
                    LOGGER.trace("New external auth: " + extAuthUri);
                    // Not sure if this is needed in the map
                    uniqueUris.put(extAuthUri, extAuthUri);                
                    uniqueExtAuths.put(key, extAuthUri);
                }
            }
            
            if (resourceCount == TimerUtils.NUM_ITEMS_TO_TIME) {
                LOGGER.info("Deduped " + resourceCount + " resources. " 
                        + TimerUtils.getDuration(resourceStart));
                resourceCount = 0;
                resourceStart = Instant.now();
            }                       
        }

        if (resourceCount > 0) {
            LOGGER.info("Deduped " + resourceCount + " resources. " 
                    + TimerUtils.getDuration(resourceStart));       
        }
        
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("uniqueUris map:");
            for (Map.Entry<String, String> entry : uniqueUris.entrySet()) {
                LOGGER.trace(entry.getKey() + " => " + entry.getValue());
            }
            LOGGER.trace("uniqueLocalAuths map:");
            for (Map.Entry<String, String> entry : 
                    uniqueLocalAuths.entrySet()) {
                LOGGER.trace(entry.getKey() + " => " + entry.getValue());
            }
        }
        
        return uniqueUris;        
    }
    
    protected Query getQuery(BfType type) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        String commandText = 
                /* NB We're not getting the authority label because we assume 
                 * it's identical to the agent label, so we can use that to 
                 * dedupe the authorities as well. If that turns out to be 
                 * wrong, we'll need to normalize the auth labels and dedupe 
                 * authorities against them directly.
                 */
                // "SELECT ?localAuth ?authAccessPt ?label ?extAuth 
                // ?extAuthLabel "
                "SELECT ?localAuth ?authAccessPoint ?label ?extAuth "
                + "WHERE { "
                + "?localAuth a ?type . "
                + "OPTIONAL { ?localAuth "
                + BfProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() + " "
                + "?authAccessPoint . } "
                + "OPTIONAL { ?localAuth  " 
                + BfProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "OPTIONAL { ?localAuth " 
                + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?extAuth . "
                // + "?externalAuth " 
                // + OntologyProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
                // + " ?extAuthLabel . "
                + "} }";

        pss.setCommandText(commandText);
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        LOGGER.trace(pss.toString());
        return pss.asQuery();            

    }

}
