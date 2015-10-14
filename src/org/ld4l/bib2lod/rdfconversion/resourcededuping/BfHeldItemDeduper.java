package org.ld4l.bib2lod.rdfconversion.resourcededuping;

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

public class BfHeldItemDeduper extends BfResourceDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfHeldItemDeduper.class);

    public BfHeldItemDeduper(OntType type) {
        super(type);
    }  
    
    @Override
    public Map<String, String> dedupe(Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());

        // Maps local URIs in the Bibframe RDF to deduped URIs (single, unique
        // URI per bf:HeldItem). This map will be used to replace duplicate URIs 
        // for the same individual with a single, unique URI.
        Map<String, String> uniqueUris = new HashMap<String, String>();
        
        // Maps keys for HeldItem identity matching to the unique HeldItem URIs. 
        Map<String, String> uniqueItems = new HashMap<String, String>();

        // Execute the query
        Query query = getQuery();        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        // Loop through query results
        while (results.hasNext()) {
            
            QuerySolution soln = results.next();
            
            String itemUri = soln.getResource("item").getURI();
            
            // Get key for agent identity matching.
            String key = getKey(soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.debug("No key for " + itemUri + "; can't dedupe");
                continue;
            }

            LOGGER.debug("Original uri: " + itemUri + " has key " + key);         

            // We've seen this HeldItem before
            if (uniqueItems.containsKey(key)) {
                
                String uniqueItemUri = uniqueItems.get(key);
                LOGGER.debug("Found matching value for key " + key 
                        + " and item URI " + itemUri);
                LOGGER.debug("Adding: " + itemUri + " => " + uniqueItemUri);                
                // This local Agent URI will be replaced by the unique Agent URI
                // throughout the data
                uniqueUris.put(itemUri, uniqueItemUri);
                
            } else {
                // We haven't seen this HeldItem before
                LOGGER.debug("New item: " + itemUri);
                // Not sure if this is needed in the map
                uniqueUris.put(itemUri, itemUri);
                uniqueItems.put(key, itemUri);
            }
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("uniqueUris map:");
            for (String uri : uniqueUris.keySet()) {
                LOGGER.debug(uri + " => " + uniqueUris.get(uri));
            }
        }
        
        return uniqueUris;        
    }
    
    protected Query getQuery() {

        /*
         * Note that we can't dedupe HeldItems on the basis of the Instance
         * they are related to by bf:holdingFor, since an Instance may have
         * multiple holdings. Even if we deduped HeldItems along with Instances
         * (as we do Agents and Authorities, for example), within the current
         * implementation we'd still have to check for matching shelfMarks.
         */
        String queryString = 
                "SELECT ?item ?lcc ?ddc ?nlm ?udc ?barcode ?id "
                + "?shelfMark ?scheme "
                + "WHERE { "
                /*
                 * We really only want to match one of these OPTIONALS if 
                 * previous ones haven't been matched. This could be done by
                 * using the same variable for the objects, and adding a filter
                 * FILTER ( ! BOUND (?object) ). But filtering is done only 
                 * after pattern matching, so it won't improve efficiency, and
                 * the query will become extremely complex. We manage the
                 * constraint in processing of the query result instead.
                 */
                + "?item a " + OntType.BF_HELD_ITEM.sparqlUri() + " . "
                + "OPTIONAL { ?item "
                + OntProperty.BF_SHELF_MARK_LCC.sparqlUri() + " ?lcc . } "
                + "OPTIONAL { ?item " 
                + OntProperty.BF_SHELF_MARK_DDC.sparqlUri() + " ?ddc . } "
                + "OPTIONAL { ?item "
                + OntProperty.BF_SHELF_MARK_NLM.sparqlUri() + " ?nlm . } "
                + "OPTIONAL { ?item " 
                + OntProperty.BF_SHELF_MARK_UDC.sparqlUri() + " ?udc . } "
                + "OPTIONAL { ?item " 
                + OntProperty.BF_BARCODE.sparqlUri() + " ?barcode . } "
                + "OPTIONAL { ?item " 
                + OntProperty.BF_ITEM_ID.sparqlUri() + " ?id . } "
                + "OPTIONAL { ?item " 
                + OntProperty.BF_SHELF_MARK.sparqlUri() + " ?shelfMark ; "
                + OntProperty.BF_SHELF_MARK_SCHEME.sparqlUri() + " ?scheme . } "        
                + "}";
        
        LOGGER.debug("BF_HELD_ITEM QUERY: " + queryString);
        return QueryFactory.create(queryString);
    }
    
    @Override
    protected String getKey(QuerySolution soln) {
        
        // Order is crucial here
        String[] keys = { "lcc", "ddc", "nlm", "udc", "barcode", "id" };
        for (String k : keys) {
            Literal lit = soln.getLiteral(k);
            if (k != null) {
                return lit.getLexicalForm();
            }
        }
        
        // If matching on shelf mark, the shelf mark scheme must also match.
        Literal mark = soln.getLiteral("shelfMark");
        if (mark != null) {
            Literal scheme = soln.getLiteral("scheme");
            if (scheme != null) {
                return scheme.getLexicalForm() + mark.getLexicalForm();
            }
        }
        
        return null;
        
    }

}
