package org.ld4l.bib2lod.rdfconversion.urigetter;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfInstanceUriGetter extends ResourceUriGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfInstanceUriGetter.class);

    private static ParameterizedSparqlString resourceSubModelPss = 
                new ParameterizedSparqlString(
                        "CONSTRUCT { ?resource ?p1 ?o1 . "
                        + " ?s ?p2 ?resource . " 
                        + " ?o1 ?p3 ?o2 "
                        + "} WHERE { { "  
                        + "?resource ?p1 ?o1 . "
                        + "OPTIONAL { " 
                        + "?o1 a " + BfType.BF_IDENTIFIER.sparqlUri() + " . "
                        + "?o1 ?p3 ?o2 . } "
                        + "} UNION { "
                        + "?s ?p2 ?resource . "
                        + "} } ");
    
    private static String sparql = 
            "PREFIX fn: <http://www.w3.org/2005/xpath-functions#>  " 
            // + "PREFIX afn: <http://jena.apache.org/ARQ/function#>  "
            + "SELECT ?instance ?worldcatId "
            + "?otherId ?otherIdScheme ?otherIdValue "
            + "WHERE { "
            + "?instance a " + BfType.BF_INSTANCE.sparqlUri() + " . "
            + "OPTIONAL { ?instance "
            + BfProperty.BF_SYSTEM_NUMBER.sparqlUri() + " "
            + "?worldcatId .  "
            
            // Can't use this because Jena doesn't recognize the namespace when
            // the local name starts with a digit.
            // + "FILTER ( afn:namespace(?worldcatId) = "
            // + "\"" + Vocabulary.WORLDCAT.uri() + "\"  ) } "
            
            + "FILTER ( fn:starts-with(str(?worldcatId), "
            + "\"" + Vocabulary.WORLDCAT.uri() + "\"  ) ) } "

            + "OPTIONAL { ?instance ?p ?otherId . "
            + "?otherId " 
            + BfProperty.BF_IDENTIFIER_SCHEME.sparqlUri() + " "
            + "?otherIdScheme ; " 
            + BfProperty.BF_IDENTIFIER_VALUE.sparqlUri() + " "
            + " ?otherIdValue . }  "
            + "}";
    
    
    public BfInstanceUriGetter(Resource resource, String localNamespace) {
        super(resource, localNamespace);
    }

    @Override
    ParameterizedSparqlString getResourceSubModelPss(Resource resource) {
        LOGGER.debug(resourceSubModelPss);
        return resourceSubModelPss;
    }
    
    @Override
    protected String getUniqueKey() {
        
        String key = null;
        
        RdfProcessor.printModel(resource.getModel(), "Instance submodel;");
        
        LOGGER.debug("Instance query: " + sparql);
        QueryExecution qexec = 
                QueryExecutionFactory.create(sparql, resource.getModel());
        ResultSet results = qexec.execSelect();
        
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            LOGGER.debug("Query solution for resource " + resource.getURI()
                    + ": " + soln.toString());
            RDFNode worldcatId = soln.get("worldcatId");
            if (worldcatId != null && worldcatId.isResource()) {
                key = worldcatId.asResource().getURI();
                LOGGER.debug("Got bf:Instance key from worldcat id " + key 
                        + " for resource " + resource.getURI());
                break;
            }
            
            RDFNode idValue = soln.get("otherIdValue");
            if (idValue != null && idValue.isLiteral()) {
                String id = idValue.asLiteral().getLexicalForm();
                RDFNode idScheme = soln.get("otherIdScheme");
                if (idScheme != null && idScheme.isResource()) {
                    String scheme = idScheme.asResource().getURI();
                    key = scheme + id;
                    LOGGER.debug("Got bf:Instance key from id scheme " + scheme 
                            + " and value "
                            + id + " for resource " 
                            + resource.getURI());
                    break;
                }
            }
        }
        
        qexec.close();
        
        if (key == null) {
            key = super.getUniqueKey();
        }
        
        return key;

    }
}
