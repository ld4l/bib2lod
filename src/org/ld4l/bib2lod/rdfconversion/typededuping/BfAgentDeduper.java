package org.ld4l.bib2lod.rdfconversion.typededuping;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntologyProperty;
import org.ld4l.bib2lod.rdfconversion.OntologyType;

public class BfAgentDeduper extends TypeDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfAgentDeduper.class);

 
    @Override
    public Map<String, String> dedupe(OntologyType type, Model model) {
        
        LOGGER.debug("Deduping type " + type.toString());

        Map<String, String> uniqueUris = new HashMap<String, String>();
        Map<String, String> labelToAgent = new HashMap<String, String>();
        Map<String, String> labelToAuth = new HashMap<String, String>();
        
        Query query = getQuery(type);        
        QueryExecution qexec = QueryExecutionFactory.create(query, model);
        ResultSet results = qexec.execSelect();
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String agentUri = soln.getResource("agent").getURI();
            String label = getAgentLabel(soln);
            
            // Without a label there's nothing to dedupe on.
            if (label == null) {
                return uniqueUris;
            }

            LOGGER.debug("Original uri: " + agentUri + " => " + label);
            label = normalizeAuthorityName(label);
            
            Resource auth = soln.getResource("auth");
            String authUri = auth == null ? null : auth.getURI();            

            if (labelToAgent.containsKey(label)) {
                // We've seen this individual before
                String uniqueAgentUri = labelToAgent.get(label);
                LOGGER.debug("Found matching value for label " + label 
                        + " and agent URI " + agentUri);
                LOGGER.debug("Adding: " + agentUri + " => " + uniqueAgentUri);
                uniqueUris.put(agentUri, uniqueAgentUri);
                // NB The algorithm assumes the agent label and the authority
                // label will always be identical. If not, we should normalize
                // auth labels and dedupe authorities on them, rather than on 
                // the agent labels.
                if (authUri != null) {
                    if (labelToAuth.containsKey(label)) {
                        String uniqueAuthUri = labelToAuth.get(label);
                        LOGGER.debug("Found matching value for label " 
                                    + label + " and auth URI " + authUri);
                        LOGGER.debug("Adding: " + authUri + " => " 
                                    + uniqueAuthUri);
                        uniqueUris.put(authUri, uniqueAuthUri);
                    } else {
                        LOGGER.debug("Didn't find auth URI for" + label);
                        labelToAuth.put(label, authUri);
                        uniqueUris.put(authUri, authUri);
                    }
                }
            } else {
                // We haven't seen this individual before
                LOGGER.debug("New agent: " + agentUri);
                uniqueUris.put(agentUri, agentUri);
                labelToAgent.put(label, agentUri);
                if (authUri != null) {
                    LOGGER.debug("New auth: " + authUri);
                    uniqueUris.put(authUri, authUri);                
                    labelToAuth.put(label, authUri);
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
    
    private Query getQuery(OntologyType type) {

        ParameterizedSparqlString pss = new ParameterizedSparqlString();
        
        String commandText = 
                /* NB We're not getting the authority label because we assume 
                 * it's identical to the agent label, so we can use that to 
                 * dedupe the authorities as well. If that turns out to be 
                 * wrong, we'll need to normalize the auth labels and dedupe 
                 * authorities against them directly.
                 */
                // "SELECT ?agent ?authAccessPt ?label ?auth ?authLabel "
                "SELECT ?agent ?authAccessPoint ?label ?auth "
                + "WHERE { "
                + "?agent a ?type . "
                + "OPTIONAL { ?agent "
                + OntologyProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() 
                + "?authAccessPoint . } "
                + "OPTIONAL { ?agent  " 
                + OntologyProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "OPTIONAL { ?agent " 
                + OntologyProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?auth . "
    //                + "?auth " 
    //                + OntologyProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
    //                + " ?authLabel . "
                + "} }";

        pss.setCommandText(commandText);
        // Make the type substitution into the parameterized SPARQL string
        pss.setIri("type", type.uri());
        return pss.asQuery();            

    }
    
    private String getAgentLabel(QuerySolution soln) {
        // NB It's assumed that bf:authorizedAccessPoint and bf:label values 
        // are identical when both exist. If that turns out to be wrong, we 
        // need some way of resolving discrepancies.
        Literal authAccessPointLiteral = soln.getLiteral("authAccessPoint");
        String authAccessPoint = authAccessPointLiteral == 
                null ? null :  authAccessPointLiteral.getLexicalForm();
        Literal labelLiteral = soln.getLiteral("label");
        String label = 
                labelLiteral ==  null ? null : labelLiteral.getLexicalForm();
        return authAccessPoint != null ? authAccessPoint : label;
    }
    

}
