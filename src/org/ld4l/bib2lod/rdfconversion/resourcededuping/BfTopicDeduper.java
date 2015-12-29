package org.ld4l.bib2lod.rdfconversion.resourcededuping;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;
import org.ld4l.bib2lod.util.TimerUtils;

public class BfTopicDeduper extends BfResourceDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfTopicDeduper.class);

    private static final Map<String, Vocabulary> SCHEMES = 
            new HashMap<String, Vocabulary>();
    static {
        SCHEMES.put("(OCoLC)fst", Vocabulary.FAST);
    }
    
    private static final Map<String, String> SCHEME_PREFIXES = 
            new HashMap<String, String>();
    static {
        SCHEME_PREFIXES.put("(OCoLC)fst", "fst");
    }
    
    public BfTopicDeduper(BfType type) {
        super(type);
    } 
 
    @Override
    public Map<String, String> dedupe(Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());

        // Maps local URIs in the Bibframe RDF to deduped URIs (single, unique
        // URI per Topic). This map will be used to replace duplicate URIs for
        // the same individual with a single, unique URI.
        Map<String, String> uniqueUris = new HashMap<String, String>();
        
        // Maps keys for Topic identity matching to the unique Topic URIs.         
        Map<String, String> uniqueTopics = new HashMap<String, String>();
        
        // Maps keys for Authority identity matching to the unique Authority
        // URIs.        
        Map<String, String> uniqueAuths = new HashMap<String, String>();
        
        // Execute the query
        Query query = getQuery();        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        // Loop through the query results
        int resourceCount = 0;
        Instant resourceStart = Instant.now();
        
        while (results.hasNext()) {
            
            resourceCount++;
            
            QuerySolution soln = results.next();

            String localTopicUri = soln.getResource("topic").getURI();
            
            // Get an external URI for this Topic, if there is one. We will 
            // replace local topic URIs with it throughout the data. In the case 
            // of topics, we also use the external URI as the key for 
            // identity-matching, rather than relying on string matching.
            String externalTopicUri = getExternalTopicUri(soln);
            
            // Get key for identity matching.
            String key = getKey(externalTopicUri, soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.debug("No key for " + localTopicUri + "; can't dedupe");
                continue;
            }
            
            // The external Topic URI, if there is one,  will be the replacement 
            // URI.      
            String replacementUri = 
                    externalTopicUri != null ? externalTopicUri : localTopicUri;

            Resource auth = soln.getResource("auth");
            String authUri = auth != null ? auth.getURI() : null;

            if (uniqueTopics.containsKey(key)) {
                
                // We've seen this Topic before
                String uniqueTopicUri = uniqueTopics.get(key);
                LOGGER.debug("Found matching value for key " + key 
                        + " and topic URI " + localTopicUri);
                LOGGER.debug(
                        "Adding: " + localTopicUri + " => " + uniqueTopicUri);                
                // This local Topic URI will be replaced by the unique Topic URI
                // throughout the data
                uniqueUris.put(localTopicUri, uniqueTopicUri);
                
                // NB The algorithm assumes the topic key and the authority
                // key will always be identical. If not, we should normalize
                // auth labels and dedupe authorities on them, rather than on 
                // the topic keys. (See this applied in BfAuthorityDeduper.)
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
                        // Not sure if this is needed in the map
                        uniqueUris.put(authUri, authUri);
                    }
                }
                
            } else {
                // We haven't seen this Topic before
                LOGGER.debug("New topic: " + localTopicUri);
                // For Topics, we're substituting the local URI with an
                // external URI, if it exists.
                uniqueUris.put(localTopicUri, replacementUri);
                uniqueTopics.put(key, replacementUri);
                if (authUri != null) {
                    LOGGER.debug("New auth: " + authUri);
                    // Not sure if this is needed in the map
                    uniqueUris.put(authUri, authUri);                
                    uniqueAuths.put(key, authUri);
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
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("uniqueUris map:");
            for (Map.Entry<String, String> entry : uniqueUris.entrySet()) {
                LOGGER.debug(entry.getKey() + " => " + entry.getValue());
            }
            LOGGER.debug("uniqueTopics map:");
            for (Map.Entry<String, String> entry : uniqueTopics.entrySet()) {
                LOGGER.debug(entry.getKey() + " => " + entry.getValue());
            }
        }
        
        return uniqueUris;        
    }
    
    // TODO Query could be a static final field, since there's no type
    // parameterization.
    protected Query getQuery() {
        
        String queryString = 
                /* NB We're not getting the authority label because we assume 
                 * it's identical to the topic label, so we can use that to 
                 * dedupe the authorities as well. If that turns out to be 
                 * wrong, we'll need to normalize the auth labels and dedupe 
                 * authorities against them directly.
                 */
                // "SELECT ?topic ?id ?authAccessPt ?label ?auth ?authLabel "
                "SELECT ?topic ?authAccessPoint ?label ?auth  ?scheme ?id "
                + "WHERE { "
                + "?topic a " + BfType.BF_TOPIC.sparqlUri() + " . "
                + "OPTIONAL { ?topic "
                + BfProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri()
                + " ?authAccessPoint . } "
                + "OPTIONAL { ?topic " 
                + BfProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "OPTIONAL { ?topic " 
                + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?auth . "
                // The authority seems to always have a scheme, but make it
                // optional just in case.
                + "OPTIONAL { ?auth " 
                + BfProperty.MADSRDF_IS_MEMBER_OF_MADS_SCHEME.sparqlUri() 
                + " ?scheme . } " 
                // + "?auth " 
                // + OntologyProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
                // + " ?authLabel . "
                + "} "
                + "OPTIONAL { ?topic "
                + BfProperty.BF_SYSTEM_NUMBER.sparqlUri()
                + " ?identifier . "
                + "?identifier a " 
                + BfType.BF_IDENTIFIER.sparqlUri() + " ; " 
                + BfProperty.BF_IDENTIFIER_VALUE.sparqlUri() + " ?id . "
                + "} }";

        LOGGER.debug("QUERY: " + queryString);
        return QueryFactory.create(queryString);

    }
    
    /**
     * Return the Topic URI. Construct the URI from an external source, if there 
     * is one; otherwise use the local URI from the RDF.
     * @param soln
     * @return Topic URI string
     */   
    private String getExternalTopicUri(QuerySolution soln) {
     
        Literal idLiteral = soln.getLiteral("id");
        if (idLiteral != null) {
            String id = idLiteral.getLexicalForm(); 
            // Currently we have only FAST URIs, but later there may be other
            // schemes that provide dereferenceable URIs.
            for (Entry<String, Vocabulary> entry : SCHEMES.entrySet()) {
                String vocabId = entry.getKey();           
                if (id.startsWith(vocabId)) {
                    // Valid XML local names don't start with a digit; see
                    // http://www.w3.org/TR/xml11/#NT-NameStartChar. FAST URIs
                    // with local name "fst" + FAST id resolve correctly, so 
                    // use that form here.
                    String prefix = SCHEME_PREFIXES.containsKey(vocabId) ?
                            SCHEME_PREFIXES.get(vocabId) : "";
                    String localName = prefix + id.substring(vocabId.length());
                            
                    String externalUri = entry.getValue().uri() + localName; 
                    LOGGER.debug(
                            "Topic URI from external scheme: " + externalUri);
                    return externalUri;
                }        
            }
        }
        
        LOGGER.debug("No external Topic URI found");
        return null;

    }
    
    private String getKey(String externalTopicUri, QuerySolution soln) {
        
        // If there's an external Topic URI, use that as the key.
        if (externalTopicUri != null) {
            return externalTopicUri;
        }

        // Otherwise derive the key from the Topic string values:
        
        // First get the default key from the authorizedAccessPoint or label
        // values.
        String key = super.getKey(soln);
        if (key != null) {
       
            // If there's a scheme, prepend it to the label, since deduping is  
            // done only relative to a scheme: we don't want to match labels in 
            // different schemes.
            Resource scheme = soln.getResource("scheme");
            if (scheme != null) {
                key = scheme.getURI() + key;
            }
        }
        
        return key;
    }
    
}
