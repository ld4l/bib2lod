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
        
        LOGGER.debug("Deduping type " + type.toString());

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
                LOGGER.debug("No key for " + localAuthUri + "; can't dedupe");
                continue;
            }

            LOGGER.debug("Original uri: " + localAuthUri + " has key " 
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
                LOGGER.debug("Found matching value for key " + localAuthKey 
                        + " and bf:Authority URI " + localAuthUri);
                LOGGER.debug("Adding: " + localAuthUri + " => " 
                        + uniqueLocalAuthUri);                
                // This local Authority URI will be replaced by the unique 
                // Authority URI throughout the data.
                uniqueUris.put(localAuthUri, uniqueLocalAuthUri);
                
                
            } else {
                // We haven't seen this local Authority before
                LOGGER.debug("New local auth: " + localAuthUri);
                LOGGER.debug("Adding: " + localAuthKey + " => " 
                        + localAuthUri);
                uniqueLocalAuths.put(localAuthKey, localAuthUri);
            }
            
            // Dedupe an associated external Authority
            if (extAuthKey != null) {
                
                // We've seen this external Authority before
                if (uniqueExtAuths.containsKey(extAuthKey)) {
                    String uniqueExtAuthUri = uniqueExtAuths.get(extAuthKey);
                    LOGGER.debug("Found matching value for key " 
                                + extAuthKey 
                                + " and external auth URI " + extAuthUri);
                    LOGGER.debug("Adding: " + extAuthUri + " => " 
                                + uniqueExtAuthUri);
                    
                    // This external Authority URI will be replaced by the
                    // unique Authority URI throughout the data.
                    uniqueUris.put(extAuthUri, uniqueExtAuthUri); 
                    
                } else {
                    // We haven't seen this Authority before
                    LOGGER.debug("Didn't find matching external auth URI for " 
                            + extAuthKey);
                    // Add the external Authority URI to the maps
                    LOGGER.debug("Adding: " + extAuthKey + " => " 
                            + extAuthUri);
                    uniqueExtAuths.put(extAuthKey, extAuthUri);
                }
            }                                                        
            
//            if (resourceCount == TimerUtils.NUM_ITEMS_TO_TIME) {
//                LOGGER.info("Deduped " + resourceCount + " "
//                        + Bib2LodStringUtils.simplePlural("file", resourceCount) 
//                        + ". " + TimerUtils.getDuration(resourceStart));
//                resourceCount = 0;
//                resourceStart = Instant.now();
//            }                       
        }
        qexec.close();

//        if (resourceCount > 0) {
//            LOGGER.info("Deduped " + resourceCount + " "
//                    + Bib2LodStringUtils.simplePlural("file", resourceCount) 
//                    + ". " + TimerUtils.getDuration(resourceStart));       
//        }
        
        if (LOGGER.isTraceEnabled()) {
            LOGGER.debug("uniqueUris map:");
            for (Map.Entry<String, String> entry : uniqueUris.entrySet()) {
                LOGGER.debug(entry.getKey() + " => " + entry.getValue());
            }
            LOGGER.debug("uniqueLocalAuths map:");
            for (Map.Entry<String, String> entry : 
                    uniqueLocalAuths.entrySet()) {
                LOGGER.trace(entry.getKey() + " => " + entry.getValue());
            }
        }
        
        return uniqueUris;        
    }
    
    protected String getExternalAuthorityKey(
            QuerySolution soln, String localAuthKey) {
        
        LOGGER.debug("Non-person authority: extAuthKey = localAuthKey");
        return localAuthKey;
    }

    protected Query getQuery(BfType type) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        String commandText = 
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
                 + "?extAuth " 
                 + BfProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
                 + " ?extAuthLabel . "
                + "} }";

        pss.setCommandText(commandText);
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        // LOGGER.debug(pss.toString());
        return pss.asQuery();            

    }
    

}
