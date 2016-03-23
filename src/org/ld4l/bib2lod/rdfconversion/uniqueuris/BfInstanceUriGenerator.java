package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfIdentifierConverter;

public class BfInstanceUriGenerator extends BfResourceUriGenerator {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfInstanceUriGenerator.class);

    private static ParameterizedSparqlString RESOURCE_SUBMODEL_PSS = 
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
    
    
    private static String SPARQL = 
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
    
    
    public BfInstanceUriGenerator(String localNamespace) {
        super(localNamespace);
    }

    @Override
    protected ParameterizedSparqlString getResourceSubModelPss() {
        LOGGER.debug(RESOURCE_SUBMODEL_PSS);
        return RESOURCE_SUBMODEL_PSS;
    }
    
    @Override
    protected String getUniqueKey() {
        
        String key = null;
        
        // RdfProcessor.printModel(resource.getModel(), "Instance submodel:");
        
        LOGGER.debug("Instance query: " + SPARQL);
        QueryExecution qexec = 
                QueryExecutionFactory.create(SPARQL, resource.getModel());
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

    // Return triples for a LocalIlsIdentifier of the Instance, where the 
    // identifier is extracted from the URI generated by the LC converter.
    // If the local identifier was stored only in the MARC 001 field, and not 
    // in the 035 field (Harvard, Stanford), then the only way to capture it is
    // from the original URI generated by the LC converter, before it gets 
    // converted to a unique URI here. The LC converter presumably uses the 001
    // field to mint the local URI, but doesn't convert it to an identifier.
    // Cornell does store local identifiers in 035, but we handle it here for
    // the sake of uniformity. Then the original identifier generated by the
    // LC converter from the 035 field will be deleted in BfInstanceConverter.
    public Model getLocalIdentifier(Resource instance, String newUri) {

        Model model = ModelFactory.createDefaultModel();
        
        String alphaPrefix = RdfProcessor.getLocalNameAlphaPrefix();
        
        // Remove the alpha prefix that has been added to the local name.
        String localName = 
                instance.getLocalName().replaceFirst(alphaPrefix, "");
        
        // Get the initial digit string from the original local name. Removes
        // the hyphen and following characters (Harvard), and "instanceN" 
        // (Cornell, Stanford).
        Matcher matcher = Pattern.compile("\\d+").matcher(localName);
        
        if (matcher.find()) {
            String localIdentifierValue = matcher.group();
            LOGGER.debug("Adding new local identifier with value " 
                    + localIdentifierValue + " for resource "
                    + newUri);
            Resource newInstance = ResourceFactory.createResource(newUri);
            return BfIdentifierConverter.createIdentifier(
                    // The new, local identifier is linked to the instance with
                    // the bf:local property.
                    newInstance, BfProperty.BF_LOCAL.property(),
                    BfProperty.BF_IDENTIFIER_VALUE.property(),
                    localIdentifierValue, 
                    BfType.BF_IDENTIFIER.type());
        }

        return model;
    }
    
}
