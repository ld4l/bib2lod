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
import org.ld4l.bib2lod.util.NacoNormalizer;
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
            String localAuthKey = getKey(soln);
        
            // Without a key there's nothing to dedupe on.
            if (localAuthKey == null) {
                LOGGER.trace("No key for " + localAuthUri + "; can't dedupe");
                continue;
            }

            LOGGER.trace("Original uri: " + localAuthUri + " has key " 
                    + localAuthKey);
    
            Resource extAuth = soln.getResource("extAuth");
            String extAuthUri = null;
            String extAuthKey = null;
            if (extAuth != null) {
                extAuthUri = extAuth.getURI();
                extAuthKey = getExternalAuthorityKey(soln, localAuthKey);
                LOGGER.debug("extAuthKey: " + extAuthKey);
            }         

            // We've seen this local Authority before
            if (uniqueLocalAuths.containsKey(localAuthKey)) {
                
                String uniqueLocalAuthUri = uniqueLocalAuths.get(localAuthKey);
                LOGGER.trace("Found matching value for key " + localAuthKey 
                        + " and bf:Authority URI " + localAuthUri);
                LOGGER.trace("Adding: " + localAuthUri + " => " 
                        + uniqueLocalAuthUri);                
                // This local Authority URI will be replaced by the unique 
                // Authority URI throughout the data.
                uniqueUris.put(localAuthUri, uniqueLocalAuthUri);
                
                // Dedupe an associated external Authority
                if (extAuthKey != null) {
                    
                    // We've seen this external Authority before
                    if (uniqueExtAuths.containsKey(extAuthKey)) {
                        String uniqueAuthUri = uniqueExtAuths.get(extAuthKey);
                        LOGGER.trace("Found matching value for key " 
                                    + localAuthKey 
                                    + " and external auth URI " + extAuthUri);
                        LOGGER.trace("Adding: " + extAuthUri + " => " 
                                    + uniqueAuthUri);
                        
                        // This external Authority URI will be replaced by the
                        // unique Authority URI throughout the data.
                        uniqueUris.put(extAuthUri, uniqueAuthUri); 
                        
                    } else {
                        // We haven't seen this Authority before
                        LOGGER.trace("Didn't find external auth URI for" 
                                + extAuthKey);
                        // Add the external Authority URI to the maps
                        uniqueExtAuths.put(extAuthKey, extAuthUri);
                        uniqueUris.put(extAuthUri, extAuthUri);
                    }
                }
                
            } else {
                // We haven't seen this local Authority before
                LOGGER.trace("New local auth: " + localAuthUri);
                // Not sure if this is needed in the map
                uniqueUris.put(localAuthUri, localAuthUri);
                uniqueLocalAuths.put(localAuthKey, localAuthUri);
                if (extAuthKey != null) {
                    LOGGER.trace("New external auth: " + extAuthUri);
                    // Not sure if this is needed in the map
                    uniqueUris.put(extAuthUri, extAuthUri);                
                    uniqueExtAuths.put(extAuthKey, extAuthUri);
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
    
    private String getExternalAuthorityKey(
            QuerySolution soln, String localAuthKey) {
        
        LOGGER.debug("Non-person authority: extAuthKey = localAuthKey");
        return localAuthKey;
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
                "SELECT "
                + "?localAuth ?authAccessPoint ?label ?extAuth ?extAuthLabel "
                + "WHERE { "
                + "?localAuth a ?type . "
                + "OPTIONAL { ?localAuth "
                + BfProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() + " "
                + "?authAccessPoint . } "
                + "OPTIONAL { ?localAuth  " 
                + BfProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "OPTIONAL { ?localAuth " 
                + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?extAuth . "
                 + "?externalAuth " 
                 + BfProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
                 + " ?extAuthLabel . "
                + "} }";

        pss.setCommandText(commandText);
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        LOGGER.trace(pss.toString());
        return pss.asQuery();            

    }
    

}
