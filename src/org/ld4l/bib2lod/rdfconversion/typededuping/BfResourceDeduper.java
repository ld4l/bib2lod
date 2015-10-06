package org.ld4l.bib2lod.rdfconversion.typededuping;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;
import org.ld4l.bib2lod.rdfconversion.naco.NacoNormalizer;

/**
 * Default deduper: dedupes on bf:authorizedAccessPoint and/or bf:label
 * string values. Can apply to any bf:Resource which doesn't have a more 
 * specific deduper.
 * @author rjy7
 *
 */
public class BfResourceDeduper extends TypeDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfResourceDeduper.class);

 
    @Override
    public Map<String, String> dedupe(OntType type, Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());

        // Maps local URIs in the Bibframe RDF to deduped URIs (single, unique
        // URI per bf:Place). This map will be used to replace duplicate URIs 
        // for the same individual with a single, unique URI.
        Map<String, String> uniqueUris = new HashMap<String, String>();

        // Maps keys for Place identity matching to the unique Place URIs. 
        Map<String, String> uniqueAuths = new HashMap<String, String>();
        
        // Execute the query
        Query query = getQuery(type);        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        // Loop through query results
        while (results.hasNext()) {
            
            QuerySolution soln = results.next();
            
            String authUri = soln.getResource("auth").getURI();
            
            // Get key for agent identity matching.
            String key = getKey(soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.debug("No key for " + authUri + "; can't dedupe");
                continue;
            }

            LOGGER.debug("Original uri: " + authUri + " has key " + key);

            // We've seen this individual before
            if (uniqueAuths.containsKey(key)) {
                
                String uniquePlaceUri = uniqueAuths.get(key);
                LOGGER.debug("Found matching value for key " + key 
                        + " and authority URI " + authUri);
                LOGGER.debug("Adding: " + authUri + " => " + uniquePlaceUri);                
                // This local Place URI will be replaced by the unique Place URI
                // throughout the data
                uniqueUris.put(authUri, uniquePlaceUri);
                                
            } else {
                // We haven't seen this Agent before
                LOGGER.debug("New authority: " + authUri);
                uniqueUris.put(authUri, authUri);
                uniqueAuths.put(key, authUri);
            }
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("uniqueUris map:");
            for (String uri : uniqueUris.keySet()) {
                LOGGER.debug(uri + " => " + uniqueUris.get(uri));
            }
            LOGGER.debug("uniquePlaces map:");
            for (String key : uniqueAuths.keySet()) {
                LOGGER.debug(key + " => " + uniqueAuths.get(key));
            }
        }
        
        return uniqueUris;        
    }
    
    private Query getQuery(OntType type) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        String commandText = 
                "SELECT ?auth ?label "
                + "WHERE { "
                + "?auth a ?type . "
                + "OPTIONAL { ?authority "
                + OntProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() + " "
                + "?authAccessPoint . } "
                + "OPTIONAL { ?auth  " 
                + OntProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "}";

        pss.setCommandText(commandText);
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        return pss.asQuery();          

    }
    
    private String getKey(QuerySolution soln) {   
        String key = getDefaultAuthorityKey(soln);
        return NacoNormalizer.normalize(key);
    }
    
}
