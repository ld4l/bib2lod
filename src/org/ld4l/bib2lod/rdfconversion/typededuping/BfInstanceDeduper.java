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

public class BfInstanceDeduper extends TypeDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfInstanceDeduper.class);

    @Override
    public Map<String, String> dedupe(OntologyType type, Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());
        
        Map<String, String> uniqueUris = new HashMap<String, String>();
        Map<String, String> uniqueInstances = new HashMap<String, String>();
        
        Query query = getQuery();        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String instanceUri = soln.getResource("instance").getURI();
            String key = getInstanceKey(soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.debug("No key for " + instanceUri + "; can't dedupe");
                continue;
            }      

            // TODO *** We may want to substitute the worldcat uri for the local
            // one - pending response from Steven
            
            if (uniqueInstances.containsKey(key)) {
                // We've seen this individual before
                String uniqueInstanceUri = uniqueInstances.get(key);
                LOGGER.debug("Found matching value for key " + key 
                        + " and instance URI " + instanceUri);
                LOGGER.debug("Adding: " + instanceUri + " => " + uniqueInstanceUri);
                uniqueUris.put(instanceUri, uniqueInstanceUri);

            } else {
                // We haven't seen this individual before
                LOGGER.debug("New instance: " + instanceUri);
                uniqueUris.put(instanceUri, instanceUri);
                uniqueInstances.put(key, instanceUri);
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
                "PREFIX fn: <http://www.w3.org/2005/xpath-functions#>  " + 
                " SELECT ?instance ?worldcatId "
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
        LOGGER.debug("No worldcat id; cannot dedupe");
        return null;
    }

}
