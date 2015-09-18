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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntologyProperty;
import org.ld4l.bib2lod.rdfconversion.OntologyType;

public class MadsrdfAuthorityDeduper extends TypeDeduper {
    
    private static final Logger LOGGER =          
            LogManager.getLogger(MadsrdfAuthorityDeduper.class);
    private static final Query QUERY = QueryFactory.create(
            "SELECT ?s ?label "
            + "WHERE { "
            + "?s a " + OntologyType.MADSRDF_AUTHORITY.sparqlUri() +  "; "
            + OntologyProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
            + "?label . "
            + "}"
    );

    @Override
    public Map<String, String> dedupe(Model model) {
        
        LOGGER.debug("Query: " + QUERY.toString());
        Map<String, String> uniqueUris = new HashMap<String, String>();
        Map<String, String> data = new HashMap<String, String>();
        QueryExecution qexec = QueryExecutionFactory.create(QUERY, model);
        ResultSet results = qexec.execSelect();
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String authorityUri = soln.getResource("s").getURI();
            String label = soln.getLiteral("label").getLexicalForm();
            LOGGER.debug("Original uri: " + authorityUri + " => " + label);
            label = normalizeAuthorityName(label);
            if (data.containsKey(label)) {
                LOGGER.debug("Found matching value for label " + label + " and URI " + authorityUri);
                LOGGER.debug("Adding: " + authorityUri + " => " + data.get(label));
                uniqueUris.put(authorityUri, data.get(label));
            } else {
                uniqueUris.put(authorityUri, authorityUri);
                data.put(label, authorityUri);
            }
        }
        if (LOGGER.isDebugEnabled()) {
            for (String uri : uniqueUris.keySet()) {
                LOGGER.debug(uri + " => " + uniqueUris.get(uri));
            }
        }
        return uniqueUris;  
    }

}
