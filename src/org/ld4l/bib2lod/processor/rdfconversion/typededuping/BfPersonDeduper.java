package org.ld4l.bib2lod.processor.rdfconversion.typededuping;

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
import org.ld4l.bib2lod.OntologyProperty;
import org.ld4l.bib2lod.OntologyType;

public class BfPersonDeduper extends TypeDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfPersonDeduper.class);
    private static final Query QUERY = QueryFactory.create(
            "SELECT ?s ?label "
            + "WHERE { "
            + "?s a " + OntologyType.BF_PERSON.sparqlUri() +  "; "
            + OntologyProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() + "?label . "
            + "}"
    );


    @Override
    public Map<String, String> dedupe(Model model) {

        Map<String, String> uniqueUris = new HashMap<String, String>();
        Map<String, String> data = new HashMap<String, String>();
        QueryExecution qexec = QueryExecutionFactory.create(QUERY, model);
        ResultSet results = qexec.execSelect();
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            String personUri = soln.getResource("s").getURI();
            String label = soln.getLiteral("label").getLexicalForm();
            LOGGER.debug("Original uri: " + personUri + " => " + label);
            label = normalizeAuthorityName(label);
            if (data.containsKey(label)) {
                LOGGER.debug("Found matching value for label " + label + " and URI " + personUri);
                LOGGER.debug("Adding: " + personUri + " => " + data.get(label));
                uniqueUris.put(personUri, data.get(label));
            } else {
                uniqueUris.put(personUri, personUri);
                data.put(label, personUri);
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
