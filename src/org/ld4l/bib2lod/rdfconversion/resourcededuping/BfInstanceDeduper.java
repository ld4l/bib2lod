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
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;

/*
 * NB Possibly we only need to dedupe Instance URIs within a single bib record,
 * not across records. In that case, it would be far more efficient to dedupe
 * in a preprocessing step on the MARCXML, on a record-by-record basis, rather
 * than deduping across entire dataset.
 */
public class BfInstanceDeduper extends BfResourceDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfInstanceDeduper.class);
        
    private static final String WORLDCAT_NS = "http://www.worldcat.org/oclc/";

    public BfInstanceDeduper(OntType type) {
        super(type);
    } 
   
    @Override
    public Map<String, String> dedupe(Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());
        
        // Maps local URIs in the Bibframe RDF to deduped URIs (single, unique
        // URI per Instance, Identifier, and Title). This map will be used to 
        // replace duplicate URIs for the same entities with a single, unique 
        // URI.
        Map<String, String> uniqueUris = new HashMap<String, String>();
        
        // Maps keys for Instance identity matching to the unique Topic URIs.
        // Note that this is also a worldcat URI to local unique URI mapping.
        Map<String, String> uniqueInstances = new HashMap<String, String>();

//        // Maps keys for Identifier identity matching to the unique Identifier
//        // URIs.
//        Map<String, String> uniqueIdentifiers = new HashMap<String, String>();
//        
//        // Maps keys for Title identity matching to the unique Title
//        // URIs.
//        Map<String, String> uniqueTitles = new HashMap<String, String>();

        // Execute the query
        Query query = getQuery();        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        // Loop through the query results
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String instanceUri = soln.getResource("instance").getURI();
            String key = getKey(soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.debug("No key (worldcat id) for " + instanceUri 
                        + "; can't dedupe");
                continue;
            }   
            
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
                // Not sure if this is needed in the map
                uniqueUris.put(instanceUri, instanceUri);
                uniqueInstances.put(key, instanceUri);
            }
        }
        
        // NB Since Instance keys are always WorldCat URIs, we can always add
        // the sameAs assertion. If we add other types of keys, we'll have to 
        // test here that the map key is a URI.
        for (Map.Entry<String, String> entry : uniqueInstances.entrySet()) {
            Resource subject = 
                    model.createResource(entry.getValue());
            Resource object = model.createResource(entry.getKey()); 
            LOGGER.debug("Adding " + subject.getURI() 
                    + " owl:sameAs " + object.getURI());
            newAssertions.add(subject, OWL.sameAs, object);            
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("uniqueUris map:");
            for (Map.Entry<String, String> entry : uniqueUris.entrySet()) {
                LOGGER.debug(entry.getKey() + " => " + entry.getValue());
            }
            LOGGER.debug("uniqueInstances map:");            
            for (Map.Entry<String, String> entry : uniqueInstances.entrySet()) {
                LOGGER.debug(entry.getKey() + " => " + entry.getValue());
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
                // + "?nonWorldCatId ?nonWorldCatIdScheme ?nonWorldCatIdValue "
                + "WHERE { "
                + "?instance a " + OntType.BF_INSTANCE.sparqlUri() + " . "
                + "OPTIONAL { ?instance "
                + OntProperty.BF_SYSTEM_NUMBER.sparqlUri() + " "
                + "?worldcatId .  "
                + "FILTER ( fn:starts-with(str(?worldcatId), "
                + "\"" + WORLDCAT_NS + "\"  ) ) } "
                // + "OPTIONAL { ?instance "
                // + OntProperty.BF_SYSTEM_NUMBER.sparqlUri() + " "
                // + "?nonWorldCatId . "
                // + "?nonWorldCatId " 
                // + OntProperty.BF_IDENTIFIER_SCHEME.sparqlUri() + " "
                // + "?nonWorldCatIdScheme ; " 
                // + OntProperty.BF_IDENTIFIER_VALUE.sparqlUri() + " "
                // + " ?nonWorldCatIdValue ; "
                // + "FILTER ( ! fn:starts-with(str(?nonWorldCatId), "
                // + "\"" + WORLDCAT_NS + "\"  ) ) } "
                + "}";

        

//        + "?s1 ?p1 ?o1 . "
//        + "?s1 a ?type . "
//        + "?o1 ?p2 ?o2 . "
//        + "?o1 a " + OntType.BF_TITLE.sparqlUri()
        
        
        LOGGER.debug("QUERY: " + queryString);
        return QueryFactory.create(queryString);

    }
    
    @Override
    protected String getKey(QuerySolution soln) {
        
        // Dedupe instances on worldcat id
        Resource worldcatId = soln.getResource("worldcatId");
        if (worldcatId != null) {
            return worldcatId.getURI();
        }
        return null;
    }
    
//    private String getIdentifierKey(QuerySolution soln) {
//        
//        // Dedupe Instance Identifiers on scheme and value
//        Resource id = soln.getResource("id");
//        if (id != null) {
//            Resource scheme = soln.getResource("idScheme");
//            String schemeUri = scheme == null ? "" : scheme.getURI();
//            Literal valueLiteral = soln.getLiteral("idValue");
//            String value = valueLiteral == null ? "" : valueLiteral.getLexicalForm();
//            return schemeUri + value;
//        }
//        return null;
//    }

}
