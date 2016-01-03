package org.ld4l.bib2lod.rdfconversion.resourcededuping;

import java.time.Instant;
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
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.util.Bib2LodStringUtils;
import org.ld4l.bib2lod.util.TimerUtils;

public class BfHeldItemDeduper extends BfResourceDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfHeldItemDeduper.class);

    public BfHeldItemDeduper(BfType type) {
        super(type);
    }  
    
    @Override
    // TODO - do we need to override BfResourceDeduper.dedupe()? What's 
    // different here?
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
        int resourceCount = 0;
        Instant resourceStart = Instant.now();
        
        while (results.hasNext()) {
            
            resourceCount++;
            
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
                // This local HeldItem URI will be replaced by the unique 
                // HeldItem URI throughout the data.
                uniqueUris.put(itemUri, uniqueItemUri);
                
            } else {
                // We haven't seen this HeldItem before
                LOGGER.debug("New item: " + itemUri);
                uniqueItems.put(key, itemUri);
            }
            
//            if (resourceCount == TimerUtils.NUM_ITEMS_TO_TIME) {
//                LOGGER.info("Deduped " + resourceCount + " "
//                        + Bib2LodStringUtils.simplePlural("file", resourceCount) 
//                        + ". " + TimerUtils.getDuration(resourceStart));
//                resourceCount = 0;
//                resourceStart = Instant.now();
//            } 
            
        }

//        if (resourceCount > 0) {
//            LOGGER.info("Deduped " + resourceCount + " "
//                    + Bib2LodStringUtils.simplePlural("file", resourceCount) 
//                    + ". " + TimerUtils.getDuration(resourceStart));    
//        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("uniqueUris map:");
            for (Map.Entry<String, String> entry : uniqueUris.entrySet()) {
                LOGGER.debug(entry.getKey() + " => " + entry.getValue());
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
                + "?item a " + BfType.BF_HELD_ITEM.sparqlUri() + " . "
                + "OPTIONAL { ?item "
                + BfProperty.BF_SHELF_MARK_LCC.sparqlUri() + " ?lcc . } "
                + "OPTIONAL { ?item " 
                + BfProperty.BF_SHELF_MARK_DDC.sparqlUri() + " ?ddc . } "
                + "OPTIONAL { ?item "
                + BfProperty.BF_SHELF_MARK_NLM.sparqlUri() + " ?nlm . } "
                + "OPTIONAL { ?item " 
                + BfProperty.BF_SHELF_MARK_UDC.sparqlUri() + " ?udc . } "
                + "OPTIONAL { ?item " 
                + BfProperty.BF_BARCODE.sparqlUri() + " ?barcode . } "
                + "OPTIONAL { ?item " 
                + BfProperty.BF_ITEM_ID.sparqlUri() + " ?id . } "
                + "OPTIONAL { ?item " 
                + BfProperty.BF_SHELF_MARK.sparqlUri() + " ?shelfMark ; "
                + BfProperty.BF_SHELF_MARK_SCHEME.sparqlUri() + " ?scheme . } "        
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
            if (lit != null) {
                LOGGER.debug("Deduping on key type " + k + " with value " 
                        + lit.getLexicalForm());          
                return lit.getLexicalForm();
            } else {
                LOGGER.debug("No value for " + k);
            }
        }
        
        // If matching on shelf mark, the shelf mark scheme must also match.
        Literal mark = soln.getLiteral("shelfMark");
        if (mark != null) {
            Literal scheme = soln.getLiteral("scheme");
            if (scheme != null) {
                LOGGER.debug("Deduping on shelfMark and shelfMarkScheme");
                return scheme.getLexicalForm() + mark.getLexicalForm();
            } else {
                LOGGER.debug("No value for shelfMark and shelfMarkScheme");
            }
        }
        
        return null;
        
    }

}
