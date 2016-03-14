package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import java.util.List;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.util.NacoNormalizer;

public class BfAuthorityUriGenerator extends BfResourceUriGenerator {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfAuthorityUriGenerator.class);

    private static ParameterizedSparqlString RESOURCE_SUBMODEL_PSS = 
            new ParameterizedSparqlString(
                    "CONSTRUCT { ?resource ?p1 ?o1 . "
                    + " ?o1 ?p2 ?o2 . "
                    + " ?s ?p3 ?resource . " 
                    + "} WHERE { { "  
                    + "?resource ?p1 ?o1 . "
                    + "OPTIONAL { ?o1 ?p2 ?o2 . } "
                    + "} UNION { " 
                    + "?s ?p3 ?resource . "
                    + "} } ");
    
    private static ParameterizedSparqlString authLabelPss = 
            new ParameterizedSparqlString(
                      "SELECT ?authLabel WHERE { "
                      + "?resource " 
                      + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " "
                      + "?auth . "
                      + "?auth a " 
                      + BfType.MADSRDF_AUTHORITY.sparqlUri() + " . "
                      + "?auth "
                      + BfProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() + " "                                  
                      + "?authLabel . "
                      + "} ");
    
    
    public BfAuthorityUriGenerator(String localNamespace) {
        super(localNamespace);
    }

    @Override
    protected ParameterizedSparqlString getResourceSubModelPss() {
        return RESOURCE_SUBMODEL_PSS;
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
        
        if (key == null) {
            key = super.getUniqueKey();
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
        authLabelPss.setIri("resource", resource.getURI());
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
            if (node != null && node.isLiteral()) {
                authoritativeLabel = node.asLiteral().getLexicalForm();
                break;
            }
        }
    
        qexec.close();
        
        authoritativeLabel = NacoNormalizer.normalize(authoritativeLabel);
        LOGGER.debug("Got authorizedLabel key " + authoritativeLabel
                + " from madsrdf:Authority for resource " + resource.getURI());
        
        return authoritativeLabel;
    } 

}
