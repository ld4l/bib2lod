package org.ld4l.bib2lod.rdfconversion.resourcededuping;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfWorkDeduper extends BfResourceDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfWorkDeduper.class);
    
    public BfWorkDeduper(OntType type) {
        super(type);
    } 
    
    @Override
    protected Query getQuery(OntType type) {

        String queryString = 
                "SELECT ?resource ?authAccessPoint ?label "
                + "WHERE { "
                + "?resource a " + OntType.BF_WORK.sparqlUri() + " . "
                + "OPTIONAL { ?resource "
                + OntProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() + " "
                + "?authAccessPoint . " 
                + "FILTER ( ! langMatches( lang(?authAccessPoint), 'x-bf-hash' ) ) } "
                + "OPTIONAL { ?resource  " 
                + OntProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "}";
        
        LOGGER.debug("BF_WORK QUERY: " + queryString);
        return QueryFactory.create(queryString);
    }

}
