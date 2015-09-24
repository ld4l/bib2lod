package org.ld4l.bib2lod.rdfconversion.typededuping;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntologyProperty;
import org.ld4l.bib2lod.rdfconversion.OntologyType;

public class BfTopicDeduper extends TypeDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfTopicDeduper.class);

 
    @Override
    public Map<String, String> dedupe(OntologyType type, Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());
        
        Map<String, String> uniqueUris = new HashMap<String, String>();
        Map<String, String> uniqueTopics = new HashMap<String, String>();
        Map<String, String> uniqueAuths = new HashMap<String, String>();
        
        Query query = getQuery();        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String topicUri = soln.getResource("topic").getURI();
            String key = getTopicKey(soln);
            
            // TODO Following is all the same as in BfAgentDeduper. We should
            // probably be able to write TypeDeduper to do all the work, with
            // subclasses just providing the query and the key.
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                return uniqueUris;
            }

            LOGGER.debug("Original uri: " + topicUri + " => " + key);
            key = normalizeAuthorityName(key);
            
            Resource auth = soln.getResource("auth");
            String authUri = auth == null ? null : auth.getURI();            

            if (uniqueTopics.containsKey(key)) {
                // We've seen this individual before
                String uniqueTopicUri = uniqueTopics.get(key);
                LOGGER.debug("Found matching value for key " + key 
                        + " and topic URI " + topicUri);
                LOGGER.debug("Adding: " + topicUri + " => " + uniqueTopicUri);
                uniqueUris.put(topicUri, uniqueTopicUri);
                // NB The algorithm assumes the topic key and the authority
                // key will always be identical. If not, we should normalize
                // auth labels and dedupe authorities on them, rather than on 
                // the topic keys.
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
                LOGGER.debug("New topic: " + topicUri);
                uniqueUris.put(topicUri, topicUri);
                uniqueTopics.put(key, topicUri);
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
    
    // TODO Query could be a static final field, since there's no type
    // parameterization.
    private Query getQuery() {
        
        String queryString = 
                /* NB We're not getting the authority label because we assume 
                 * it's identical to the topic label, so we can use that to 
                 * dedupe the authorities as well. If that turns out to be 
                 * wrong, we'll need to normalize the auth labels and dedupe 
                 * authorities against them directly.
                 */
                // "SELECT ?topic ?id ?authAccessPt ?label ?auth ?authLabel "
                "SELECT ?topic ?subjectHeading ?authAccessPoint ?label ?auth "
                + "WHERE { "
                + "?topic a " + OntologyType.BF_TOPIC.sparqlUri() + " . "
                + "OPTIONAL { ?topic "
                + OntologyProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() 
                + "?authAccessPoint . } "
                + "OPTIONAL { ?topic  " 
                + OntologyProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "OPTIONAL { ?topic " 
                + OntologyProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?auth . } "
    //                + "?auth " 
    //                + OntologyProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
    //                + " ?authLabel . "
                + "OPTIONAL { ?topic "
                + OntologyProperty.BF_SYSTEM_NUMBER.sparqlUri() + " "
                + "?id . "
                + "?id a " + OntologyType.BF_IDENTIFIER.sparqlUri() + " ; " 
                + OntologyProperty.BF_IDENTIFIER_VALUE.sparqlUri() + " ?subjectHeading . "
                + "} }";

        LOGGER.debug("QUERY: " + queryString);
        return QueryFactory.create(queryString);

    }
    
    private String getTopicKey(QuerySolution soln) {
        
        // First look for a FAST heading
        Literal subjectHeadingLiteral = soln.getLiteral("subjectHeading");
        if (subjectHeadingLiteral != null) {
            String subjectHeading = subjectHeadingLiteral.getLexicalForm(); 
            // Leave the (OCoLC)fst prefix on in case we want to use other 
            // types of identifier values as well.
            if (subjectHeading.startsWith("(OCoLC)fst")) {
                LOGGER.debug("Using FAST heading "+ subjectHeading 
                        + " to dedupe");
                return subjectHeading;
            }            
        }
       
        return getDefaultKey(soln);
    }
    

}
