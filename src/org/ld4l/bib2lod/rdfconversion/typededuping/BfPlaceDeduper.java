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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;
import org.ld4l.bib2lod.rdfconversion.naco.NacoNormalizer;

public class BfPlaceDeduper extends TypeDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfPlaceDeduper.class);

 
    @Override
    public Map<String, String> dedupe(OntType type, Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());

        // Maps local URIs in the Bibframe RDF to deduped URIs (single, unique
        // URI per bf:Place). This map will be used to replace duplicate URIs 
        // for the same individual with a single, unique URI.
        Map<String, String> uniqueUris = new HashMap<String, String>();

        // Maps keys for Place identity matching to the unique Place URIs. 
        Map<String, String> uniquePlaces = new HashMap<String, String>();
        
        // Execute the query
        Query query = getQuery();        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        // Loop through query results
        while (results.hasNext()) {
            
            QuerySolution soln = results.next();
            
            String placeUri = soln.getResource("place").getURI();
            
            // Get key for agent identity matching.
            String key = buildKey(soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.debug("No key for " + placeUri + "; can't dedupe");
                continue;
            }

            // Normalize the key to remove non-distinctive differences
            key = NacoNormalizer.normalize(key);
            LOGGER.debug("Original uri: " + placeUri + " has key " + key);

            // We've seen this Place before
            if (uniquePlaces.containsKey(key)) {
                
                String uniquePlaceUri = uniquePlaces.get(key);
                LOGGER.debug("Found matching value for key " + key 
                        + " and place URI " + placeUri);
                LOGGER.debug("Adding: " + placeUri + " => " + uniquePlaceUri);                
                // This local Place URI will be replaced by the unique Place URI
                // throughout the data
                uniqueUris.put(placeUri, uniquePlaceUri);
                                
            } else {
                // We haven't seen this Agent before
                LOGGER.debug("New agent: " + placeUri);
                uniqueUris.put(placeUri, placeUri);
                uniquePlaces.put(key, placeUri);
            }
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("uniqueUris map:");
            for (String uri : uniqueUris.keySet()) {
                LOGGER.debug(uri + " => " + uniqueUris.get(uri));
            }
            LOGGER.debug("uniquePlaces map:");
            for (String key : uniquePlaces.keySet()) {
                LOGGER.debug(key + " => " + uniquePlaces.get(key));
            }
        }
        
        return uniqueUris;        
    }
    
    private Query getQuery() {

        // TODO Store query as static final field, since not parameterized.
        String queryString = 
                "SELECT ?place ?label "
                + "WHERE { "
                + "?place a " + OntType.BF_PLACE.sparqlUri() + " ; "
                + OntProperty.BF_LABEL.sparqlUri() + " ?label . "
                + "}";

       return QueryFactory.create(queryString);          

    }
    
    private String buildKey(QuerySolution soln) {
        
        Literal label = soln.getLiteral("label");
        if (label != null) {
            LOGGER.debug("Using bf:label to dedupe");
            return label.getLexicalForm();
        }
        
        return null;
    }
    
}
