package org.ld4l.bib2lod.rdfconversion.urigetter;

import java.util.List;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.util.NacoNormalizer;

public class BfAuthorityUriGetter extends ResourceUriGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfAuthorityUriGetter.class);
 
    private static ParameterizedSparqlString authLabelPss = 
            new ParameterizedSparqlString(
                      "SELECT ?authLabel WHERE { "
                      + "?s " + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " "
                      + "?auth . "
                      + "?auth a " 
                      + BfType.MADSRDF_AUTHORITY.sparqlUri() + " . "
                      + "?auth "
                      + BfProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() + " "                                  
                      + "?authLabel . "
                      + "} ");
    
    public BfAuthorityUriGetter(Resource resource, String localNamespace) {
        super(resource, localNamespace);
    }
    
    @Override
    public String getUniqueKey() {
        
        String key = getKeyFromAuthorizedAccessPoint();
        
        // Infrequently there a madsrdf:Authority or bf:label but no
        // bf:authorizedAccessPoint.
        if (key == null) {
            key = getKeyFromAuthoritativeLabel();
        } 
        
        if (key == null) {
            key = getKeyFromBfLabel();
        }
        
        return key;
    }

   private String getKeyFromAuthorizedAccessPoint() {
        
        String authAccessPoint = null;

        List<Statement> statements = resource.listProperties(
                BfProperty.BF_AUTHORIZED_ACCESS_POINT.property()).toList();

        for (Statement statement : statements) {
            RDFNode object = statement.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                authAccessPoint = literal.getLexicalForm();
                break;
            } // If there is more than one (which shouldn't happen) we'll get 
              // the first, but doesn't matter if there are no selection
              // criteria.                
        }
    
        authAccessPoint = NacoNormalizer.normalize(authAccessPoint);
        LOGGER.debug("Got authAccessPoint key " + authAccessPoint
                + " for resource " + resource.getURI());
        
        return authAccessPoint;
    }
    
    /*
     * Get resource key from the associated madsrdf:Authority authorizedLabel.
     * NB Getting the key for the madsrdf:Authority itself is done in 
     * getMadsAuthorityKey().
     */
    private String getKeyFromAuthoritativeLabel() {
        
        String authoritativeLabel = null;

        // Easier to do this as a SPARQL query since we're jumping over the 
        // intermediate madsrdf:Authority node.
        authLabelPss.setIri("s", resource.getURI());
        LOGGER.debug(authLabelPss.toString());
        Query query = authLabelPss.asQuery();

        QueryExecution qexec = 
                QueryExecutionFactory.create(query, resource.getModel());
        ResultSet results = qexec.execSelect();
        
        // If there is more than one (which there shouldn't be), we'll get the
        // first one, but doesn't matter if we have no selection criteria. 
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            RDFNode node = soln.get("authLabel");
            if (node.isLiteral()) {
                Literal literal = node.asLiteral();
                authoritativeLabel = literal.getLexicalForm();
                break;
            }
        }
    
        authoritativeLabel = NacoNormalizer.normalize(authoritativeLabel);
        LOGGER.debug("Got authorizedLabel key " + authoritativeLabel
                + " from madsrdf:Authority for resource " + resource.getURI());
        
        return authoritativeLabel;
    } 

}
