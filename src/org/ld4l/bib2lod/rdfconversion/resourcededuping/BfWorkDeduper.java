package org.ld4l.bib2lod.rdfconversion.resourcededuping;

import org.apache.jena.query.Query;
import org.apache.jena.query.QueryFactory;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;

public class BfWorkDeduper extends BfResourceDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfWorkDeduper.class);
    
    public BfWorkDeduper(BfType type) {
        super(type);
    } 
    
    @Override
    protected Query getQuery(BfType type) {

        String queryString = 
                "SELECT ?resource ?authAccessPoint ?label "
                + "WHERE { "
                + "?resource a " + BfType.BF_WORK.sparqlUri() + " . "
                + "OPTIONAL { ?resource "
                + BfProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() + " "
                + "?authAccessPoint . " 
                + "FILTER ( ! langMatches( lang(?authAccessPoint), 'x-bf-hash' ) ) } "
                + "OPTIONAL { ?resource  " 
                + BfProperty.BF_LABEL.sparqlUri() + " ?label . } "
                + "}";
        
        LOGGER.debug("BF_WORK QUERY: " + queryString);
        return QueryFactory.create(queryString);
    }

}
