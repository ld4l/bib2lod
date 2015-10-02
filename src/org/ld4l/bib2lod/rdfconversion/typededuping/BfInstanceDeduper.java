package org.ld4l.bib2lod.rdfconversion.typededuping;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntologyProperty;
import org.ld4l.bib2lod.rdfconversion.OntologyType;

/*
 * NB Possibly we only need to dedupe Instance URIs within a single bib record,
 * not across records. In that case, it would be far more efficient to dedupe
 * in a preprocessing step on the MARCXML, on a record-by-record basis, rather
 * than deduping across entire dataset.
 */
public class BfInstanceDeduper extends TypeDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfInstanceDeduper.class);

    @Override
    public Map<String, String> dedupe(OntologyType type, Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());
        
        // Maps local URIs in the Bibframe RDF to deduped URIs (single, unique
        // URI per Instance). This map will be used to replace duplicate URIs
        // for the same Instance with a single, unique URI.
        Map<String, String> uniqueUris = new HashMap<String, String>();
        
        // Maps keys for Instance identity matching to the unique Topic URIs.
        // Note that this is also a worldcat URI to local unique URI mapping.
        Map<String, String> uniqueInstances = new HashMap<String, String>();
        
        // Execute the query
        Query query = getQuery();        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        // Loop through the query results
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String instanceUri = soln.getResource("instance").getURI();
            String key = getInstanceKey(soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.debug("No key (worldcat id) for " + instanceUri 
                        + "; can't dedupe");
                continue;
            }      

            // TODO *** We may want to substitute the worldcat uri for the local
            // one - pending response from Steven. If not, we need to add a
            // sameAs statement between the unique Instance URI and the worldcat
            // URI, so keep another map (or is that just the uniqueInstances
            // map?
            
            if (uniqueInstances.containsKey(key)) {
                // We've seen this Instance before
                String uniqueInstanceUri = uniqueInstances.get(key);
                LOGGER.debug("Found matching value for key " + key 
                        + " and instance URI " + instanceUri);
                LOGGER.debug(
                        "Adding: " + instanceUri + " => " + uniqueInstanceUri);
                // This local Instance URI will be replaced by the unique 
                // Instance URI throughout the data. 
                uniqueUris.put(instanceUri, uniqueInstanceUri);

            } else {
                // We haven't seen this Instance before
                LOGGER.debug("New instance: " + instanceUri);
                uniqueUris.put(instanceUri, instanceUri);
                uniqueInstances.put(key, instanceUri);
            }
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("uniqueUris map:");
            for (String uri : uniqueUris.keySet()) {
                LOGGER.debug(uri + " => " + uniqueUris.get(uri));
            }
            LOGGER.debug("uniqueInstances map:");
            for (String key : uniqueInstances.keySet()) {
                LOGGER.debug(key + " => " + uniqueInstances.get(key));
            }
        }
        
        return uniqueUris;        
    }
    
    // TODO Query could be a static final field, since there's no type
    // parameterization.
    private Query getQuery() {
        
        String queryString = 
                "PREFIX fn: <http://www.w3.org/2005/xpath-functions#>  " 
                + "SELECT ?instance ?worldcatId "
                + "WHERE { "
                + "?instance a " + OntologyType.BF_INSTANCE.sparqlUri() + " . "
                + "OPTIONAL { ?instance "
                + OntologyProperty.BF_SYSTEM_NUMBER.sparqlUri() + " "
                + "?worldcatId .  "
                + "FILTER ( fn:starts-with(str(?worldcatId), 'http://www.worldcat.org/oclc/' ) ) } "
                + "}";

        LOGGER.debug("QUERY: " + queryString);
        return QueryFactory.create(queryString);

    }
    
    private String getInstanceKey(QuerySolution soln) {
        
        // Dedupe instances on worldcat id
        Resource worldcatId = soln.getResource("worldcatId");
        if (worldcatId != null) {
            return worldcatId.getURI();
        }
        return null;
    }

}
