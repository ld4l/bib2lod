package org.ld4l.bib2lod.rdfconversion.resourcededuping;

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
import org.apache.jena.rdf.model.ModelFactory;
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
public class BfResourceDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfResourceDeduper.class);
    
    protected Model newStatements;
    protected OntType type;
    
    public BfResourceDeduper(OntType type) {
        LOGGER.debug("In constructor for " + this.getClass().getName());
        this.type = type;
        newStatements = ModelFactory.createDefaultModel();
    }
    
    public Map<String, String> dedupe(Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());

        // Maps local URIs in the Bibframe RDF to deduped URIs (single, unique
        // URI per bf:Place). This map will be used to replace duplicate URIs 
        // for the same individual with a single, unique URI.
        Map<String, String> uniqueUris = new HashMap<String, String>();

        // Maps keys for Place identity matching to the unique Place URIs. 
        Map<String, String> uniqueResources = new HashMap<String, String>();
        
        // Execute the query
        Query query = getQuery(type);        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        
        // Loop through query results
        while (results.hasNext()) {
            
            QuerySolution soln = results.next();
            
            String resourceUri = soln.getResource("resource").getURI();   
            
            // Get key for agent identity matching.
            String key = getKey(soln);
            
            // Without a key there's nothing to dedupe on.
            if (key == null) {
                LOGGER.debug("No key for " + resourceUri + "; can't dedupe");
                continue;
            }

            LOGGER.debug("Original uri: " + resourceUri + " has key " + key);

            // We've seen this individual before
            if (uniqueResources.containsKey(key)) {
                
                String uniqueResourceUri = uniqueResources.get(key);
                LOGGER.debug("Found matching value for key " + key 
                        + " and resource URI " + resourceUri);
                LOGGER.debug("Adding: " + resourceUri + " => " + uniqueResourceUri);                
                // This local Resource URI will be replaced by the unique 
                // Resource URI throughout the data
                uniqueUris.put(resourceUri, uniqueResourceUri);
                                
            } else {
                // We haven't seen this Resource before
                LOGGER.debug("New resource: " + resourceUri);
                // Not sure if this is needed in the map
                uniqueUris.put(resourceUri, resourceUri);
                uniqueResources.put(key, resourceUri);
            }
        }
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("uniqueUris map:");
            for (String uri : uniqueUris.keySet()) {
                LOGGER.debug(uri + " => " + uniqueUris.get(uri));
            }
            LOGGER.debug("uniqueResources map:");
            for (String key : uniqueResources.keySet()) {
                LOGGER.debug(key + " => " + uniqueResources.get(key));
            }
        }
        
        return uniqueUris;        
    }
    
    protected Query getQuery(OntType type) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        String commandText = 
                "SELECT ?resource ?authAccessPoint ?label "
                + "WHERE { "
                + "?resource a ?type . "
                + "OPTIONAL { ?resource "
                + OntProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() + " "
                + "?authAccessPoint . } "
                + "OPTIONAL { ?resource  " 
                + OntProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "}";

        pss.setCommandText(commandText);
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        LOGGER.debug(pss.toString());
        return pss.asQuery();          

    }

    protected String getKey(QuerySolution soln) {
        // NB It's assumed that bf:authorizedAccessPoint and bf:label values 
        // are identical when both exist. If that turns out to be wrong, we 
        // need some way of resolving discrepancies.
        
        String key = null;

        // Return authorizedAccessPoint value, if there is one
        Literal authAccessPoint = soln.getLiteral("authAccessPoint");
        if (authAccessPoint != null) {
            LOGGER.debug("Using bf:authorizedAccessPoint to dedupe");
            key = authAccessPoint.getLexicalForm();
            
        } else {       
            // Else return bf:label, if there is one
            Literal label = soln.getLiteral("label");
            if (label != null) {
                LOGGER.debug("Using bf:label to dedupe");
                key = label.getLexicalForm();
            }
        }
        
        return NacoNormalizer.normalize(key);
    }
   
    
    public Model getNewStatements() {
        return newStatements;
    }
    
}
