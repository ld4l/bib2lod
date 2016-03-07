package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
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
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

// TODO Will need to treat topics differently - URI should come from schemes
// like FAST. Will not just need to send back a key.
public class BfTopicUriGenerator extends BfResourceUriGenerator {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTopicUriGenerator.class);

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

    
    private static ParameterizedSparqlString SELECT_PSS = 
            new ParameterizedSparqlString(
                    "SELECT ?authAccessPoint ?bfLabel ?madsAuthScheme "  
                    + "?madsAuthLabel ?id ?authSource "
                    + "WHERE { "
                    + "?topic a " + BfType.BF_TOPIC.sparqlUri() + " . "
                    + "OPTIONAL { ?topic "
                    + BfProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri()
                    + " ?authAccessPoint . } "
                    + "OPTIONAL { ?topic " 
                    + BfProperty.BF_LABEL.sparqlUri() + " ?bfLabel . } "
                    + "OPTIONAL { ?topic " 
                    + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?madsAuth . "
                    + "?madsAuth " 
                    + BfProperty.MADSRDF_IS_MEMBER_OF_MADS_SCHEME.sparqlUri() 
                    + " "
                    + "?madsAuthScheme . " 
                    + "?madsAuth " 
                    + BfProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
                    + " ?madsAuthLabel . } "
                    + "OPTIONAL { ?topic "
                    + BfProperty.BF_SYSTEM_NUMBER.sparqlUri() + " "
                    + "?identifier . "
                    + "?identifier a " 
                    + BfType.BF_IDENTIFIER.sparqlUri() + " ; " 
                    + BfProperty.BF_IDENTIFIER_VALUE.sparqlUri() + " ?id . "
                    + "OPTIONAL { ?topic " 
                    + BfProperty.BF_AUTHORITY_SOURCE.sparqlUri() + " " 
                    + "?authSource } "
                    + "} }");

    private static final Map<String, Vocabulary> SCHEMES = 
            new HashMap<String, Vocabulary>();
    static {
        SCHEMES.put("(OCoLC)fst", Vocabulary.FAST); 
    }
    
    private static final Map<String, String> SCHEME_PREFIXES = 
            new HashMap<String, String>();
    static {
        SCHEME_PREFIXES.put("(OCoLC)fst", "fst");
    }
    
    
    public BfTopicUriGenerator(String localNamespace) {
        super(localNamespace);
    }
    
    @Override
    protected ParameterizedSparqlString getResourceSubModelPss() {
        return RESOURCE_SUBMODEL_PSS;
    }
    
    @Override
    public String getUniqueUri(Resource originalResource) {
        
        this.resource = getResourceWithSubModel(originalResource);
        
        String uri = null;
        
        SELECT_PSS.setIri("topic", resource.getURI());   
        Query query = SELECT_PSS.asQuery();
        // LOGGER.debug(query.toString());
        QueryExecution qexec = QueryExecutionFactory.create(
                query, resource.getModel());
        ResultSet results = qexec.execSelect();
        
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            
            uri = getUriFromExternalAuth(soln);            
            if (uri != null) {  
                LOGGER.debug("Got external authority URI " + uri + " for "
                        + "resource " + resource.getURI());
                // Return the external authority URI unhashed
                
                return uri;
            }
            // TODO
            // If a fast value, send back that uri - unhashed
            // otherwise get hash from Identifier value or consider the other
            // attributes: bf:label, bf:authoritySource, bf:authAccessPoint, 
            // bf:hasAuthority + madsrdf:authoritativeLabel. Also the predicate:
            // lccn etc. value + scheme
        }

        qexec.close();
        
        if (uri == null) {
            uri = super.getUniqueUri(originalResource);
        }
        return uri;
    }
    
    /*
     * Gets a Topic URI from an external Authority such as FAST. 
     * Currently FAST is the only one in the data; later there could be others.
     */
    private String getUriFromExternalAuth(QuerySolution soln) {
        
        String externalAuthUri = null;
 
        RDFNode idNode = soln.get("id");
        if (idNode != null && idNode.isLiteral()) {
            String id = idNode.asLiteral().getLexicalForm(); 
            // Currently we have only FAST URIs, but later there may be other
            // schemes that provide dereferenceable URIs.
            for (Entry<String, Vocabulary> entry : SCHEMES.entrySet()) {
                String vocabId = entry.getKey();           
                if (id.startsWith(vocabId)) {
                    // Valid XML local names don't start with a digit; see
                    // http://www.w3.org/TR/xml11/#NT-NameStartChar. FAST URIs
                    // with local name "fst" + FAST id resolve correctly, so 
                    // use that form here.
                    String prefix = SCHEME_PREFIXES.containsKey(vocabId) ?
                            SCHEME_PREFIXES.get(vocabId) : "";
                    String localName = prefix + id.substring(vocabId.length());
                            
                    externalAuthUri = entry.getValue().uri() + localName; 
                    LOGGER.debug("Topic URI from external scheme: " 
                            + externalAuthUri);
                }        
            }
        }
        
        return externalAuthUri;
    }
    
}
