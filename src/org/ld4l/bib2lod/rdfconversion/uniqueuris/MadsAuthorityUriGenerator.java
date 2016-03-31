package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.util.NacoNormalizer;

public class MadsAuthorityUriGenerator extends BfResourceUriGenerator {

    private static final Logger LOGGER = 
            LogManager.getLogger(MadsAuthorityUriGenerator.class);

    private static ParameterizedSparqlString RESOURCE_SUBMODEL_PSS = 
            new ParameterizedSparqlString(
                    "CONSTRUCT { ?resource ?p1 ?o1 . "
                    + "} WHERE {  "  
                    + "?resource ?p1 ?o1 . "
                    + "} ");
    
    private ParameterizedSparqlString MADS_AUTHORITY_PSS = 
            new ParameterizedSparqlString(
                    "SELECT ?authLabel ?madsScheme WHERE { "
                    + "?auth a " + BfType.MADSRDF_AUTHORITY.sparqlUri() + " . "
                    + "?auth " 
                    + BfProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() + " "
                    + "?authLabel . "
                    + "?auth "
                    + BfProperty.MADSRDF_IS_MEMBER_OF_MADS_SCHEME.sparqlUri()
                    + " ?madsScheme . }");
                    
    public MadsAuthorityUriGenerator(String localNamespace) {
        super(localNamespace);
    }
    
    @Override
    protected ParameterizedSparqlString getResourceSubModelPss() {
        return RESOURCE_SUBMODEL_PSS;
    }

    @Override
    protected String getUniqueKey() {
        
        String key = getUniqueKeyFromAuthLabelAndScheme();
        
        if (key == null) {
            LOGGER.debug("Getting unique key from superclass");
            key = super.getUniqueKey();
        }
        
        return key;
    }
    
    private String getUniqueKeyFromAuthLabelAndScheme() {
        
        String key = null;
        
        MADS_AUTHORITY_PSS.setIri("auth", resource.getURI());   
        Query query = MADS_AUTHORITY_PSS.asQuery();
        // LOGGER.debug(query.toString());
        
        QueryExecution qexec = QueryExecutionFactory.create(
                query, resource.getModel());
        ResultSet results = qexec.execSelect();
        
        // There should be exactly one result.
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            Resource madsScheme = soln.getResource("madsScheme");
            Literal literal = soln.getLiteral("authLabel");
            String authLabel = 
                    NacoNormalizer.normalize(literal.getLexicalForm());
            // Combining the scheme with the authoritativeLabel will distinguish
            // the mads:Authority URI from the related bf:Authority URI, which 
            // gets the unique key only from the authoritativeLabel.
            key = madsScheme.getURI() + "+" + authLabel;
            LOGGER.debug("Got unique key for MADS Authority from MADS scheme " 
                    + "and MADS authoritativeLabel: " + key);
        }

        return key;
    }
}
