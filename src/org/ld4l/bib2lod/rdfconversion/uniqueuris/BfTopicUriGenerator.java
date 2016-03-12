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
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
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
    
    // Currently this only gets FAST IDs. 
    private static ParameterizedSparqlString EXTERNAL_IDENTIFIER_PSS = 
            new ParameterizedSparqlString(
                    "SELECT ?id WHERE { "  
                    + "?topic a " + BfType.BF_TOPIC.sparqlUri() + " . "
                    + "?topic " 
                    + BfProperty.BF_SYSTEM_NUMBER.sparqlUri() + "?identifier . "                    
                    + "?identifier a " 
                    + BfType.BF_IDENTIFIER.sparqlUri() + " ; " 
                    + BfProperty.BF_IDENTIFIER_VALUE.sparqlUri() + " ?id . }");
    
    private static ParameterizedSparqlString MADS_AUTHORITY_PSS = 
            new ParameterizedSparqlString(
                    "SELECT ?madsScheme ?madsAuthLabel WHERE { "  
                    + "?topic a " + BfType.BF_TOPIC.sparqlUri() + " . "
                    + "?topic " 
                    + BfProperty.BF_HAS_AUTHORITY.sparqlUri() + " ?madsAuth . "
                    + "?madsAuth " 
                    + BfProperty.MADSRDF_IS_MEMBER_OF_MADS_SCHEME.sparqlUri() 
                    + " ?madsScheme . "
                    + "?madsAuth " 
                    + BfProperty.MADSRDF_AUTHORITATIVE_LABEL.sparqlUri() 
                    + "?madsAuthLabel . }"); 


    private static ParameterizedSparqlString AUTH_ACCESS_POINT_PSS = 
            new ParameterizedSparqlString(
                    "SELECT ?type ?authAccessPoint WHERE { "
                    + "?topic a " + BfType.BF_TOPIC.sparqlUri() + " . "
                    + "?topic a ?type . "                                      
                    + "?topic "
                    + BfProperty.BF_AUTHORIZED_ACCESS_POINT.sparqlUri() + " "
                    + "?authAccessPoint . "
                    + "FILTER (?type != " + BfType.BF_TOPIC.sparqlUri() + ") }");
    
    // Map prefix on external identifier values to the vocabulary they belong
    // to.
    private static final Map<String, Vocabulary> ID_PREFIX_TO_VOCABULARY = 
            new HashMap<String, Vocabulary>();
    static {
        ID_PREFIX_TO_VOCABULARY.put("(OCoLC)fst", Vocabulary.FAST); 
    }
    
    // Map prefix on external identifier value to the new local name prefix.
    private static final Map<String, String> ID_PREFIX_TO_LOCAL_NAME_PREFIX = 
            new HashMap<String, String>();
    static {
        ID_PREFIX_TO_LOCAL_NAME_PREFIX.put("(OCoLC)fst", "fst");
    }
    
//    private static final Map<String, Vocabulary> AUTHORITY_SOURCES = 
//            new HashMap<String, Vocabulary>();
//    static {
//        AUTHORITY_SOURCES.put(
//                "http://id.loc.gov/vocabulary/subjectSchemes/fast", 
//                Vocabulary.FAST);
//    }
    
    
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

        String uri = getUriFromExternalIdentifier();

        if (uri != null) {
            LOGGER.debug("Got external identifier URI " + uri + " for "
                    + "resource " + resource.getURI());         
            // Return the external authority URI unhashed  
            return uri;
        }
        
        // super.getUniqueLocalName() takes care of the hashing. This 
        // class overrides super.getUniqueKey which gets the content to be
        // hashed.
        String localName = getUniqueLocalName();
        uri = localNamespace + localName;

        return uri;
    }
    
    /*
     * Gets a Topic URI from an external Authority such as FAST. 
     * Currently FAST is the only one in the data; later there could be others.
     */
    private String getUriFromExternalIdentifier() {
        
        String externalIdUri = null;
        
        EXTERNAL_IDENTIFIER_PSS.setIri("topic", resource.getURI());   
        Query query = EXTERNAL_IDENTIFIER_PSS.asQuery();
        // LOGGER.debug(query.toString());
        
        QueryExecution qexec = QueryExecutionFactory.create(
                query, resource.getModel());
        ResultSet results = qexec.execSelect();
        
        // First get the URI from an external authority. For now, we have only
        // FAST URIs (mostly from Cornell, a few from Stanford).
        while (results.hasNext()) {
            QuerySolution soln = results.next();

            RDFNode idNode = soln.get("id");
            if (idNode != null && idNode.isLiteral()) {
                String id = idNode.asLiteral().getLexicalForm(); 
                
                // Currently we have only FAST Identifiers, but there are other 
                // authoritySources in the data. Examples:
                // http://id.loc.gov/vocabulary/subjectSchemes/rbgenr
                // http://id.loc.gov/vocabulary/subjectSchemes/rbprov
                // All are within the 
                // http://id.loc.gov/vocabulary/subjectSchemes/ namespace.
                // But these Topics do not have Identifiers with 
                // bf:identifierValues, like the FAST identifiers.                
                for (Entry<String, Vocabulary> entry : 
                        ID_PREFIX_TO_VOCABULARY.entrySet()) {
                    String vocabId = entry.getKey();           
                    if (id.startsWith(vocabId)) {
                        // Valid XML local names don't start with a digit; see
                        // http://www.w3.org/TR/xml11/#NT-NameStartChar. FAST 
                        // URIs with local name "fst" + FAST id resolve  
                        // correctly, so use that form here.
                        String prefix = 
                                ID_PREFIX_TO_LOCAL_NAME_PREFIX.get(vocabId);
                        String localName = 
                                prefix + id.substring(vocabId.length());
                                
                        externalIdUri = entry.getValue().uri() + localName; 
                        LOGGER.debug("Found Topic URI from an external " 
                                + "identifier. Scheme: " + vocabId + ". " 
                                + "External URI: " + externalIdUri + ".");    
                    }        
                }
            }            
        }
        
        if (LOGGER.isDebugEnabled()) {
            if (externalIdUri == null) {
                LOGGER.debug("No external Topic URI found.");
            }
        }
        return externalIdUri;
    }
    
    @Override
    protected String getUniqueKey() {
        String key = getUniqueKeyFromMadsAuthority();
        
        if (key == null) {
            key = getUniqueKeyFromTypeAndAuthAccessPoint();
        }
        
        // As a last resort, use the local name as the unique key.
        // Note that Topics should not use just a string value like bf:label,
        // bf:authorizedAccessPoint, because these are only the same within the
        // context of a given scheme.
        if (key == null) {
            key = super.getUniqueKey();
        }
        
        return key;
    }
    
    private String getUniqueKeyFromMadsAuthority() {
        
        String key = null;
        
        MADS_AUTHORITY_PSS.setIri("topic", resource.getURI());   
        Query query = MADS_AUTHORITY_PSS.asQuery();
        // LOGGER.debug(query.toString());
        
        QueryExecution qexec = QueryExecutionFactory.create(
                query, resource.getModel());
        ResultSet results = qexec.execSelect();
        
        // There should be at most one result.
        // Example:
        // madsScheme: 
        // madsAuthLabel:
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            Resource madsScheme = soln.getResource("madsScheme");
            Literal authLabel = soln.getLiteral("madsAuthLabel");
            key = madsScheme.getURI() + "+" + authLabel.getLexicalForm();
            LOGGER.debug("Got unique key from MADS scheme and " 
                    + "MADS authoritativeLabel: " + key);
        }

        return key;
    }
    
    private String getUniqueKeyFromTypeAndAuthAccessPoint() {
        String key = null;
        
        AUTH_ACCESS_POINT_PSS.setIri("topic", resource.getURI());
        Query query = AUTH_ACCESS_POINT_PSS.asQuery();
        
        QueryExecution qexec = QueryExecutionFactory.create(
                query, resource.getModel());
        ResultSet results = qexec.execSelect();

        // There should be at most one result.
        // Example:
        // type: http://www.loc.gov/mads/rdf/v1#HierarchicalGeographic
        // (This is the only type represented in the data.)
        // bf:authorizedAccessPoint: "United States. New York. New York."
        while (results.hasNext()) {
            QuerySolution soln = results.next();
            Resource type = soln.getResource("type");
            Literal authAccessPoint = soln.getLiteral("authAccessPoint");
            key = type.getURI() + "+" + authAccessPoint.getLexicalForm();
            LOGGER.debug("Got unique key from specialized Topic type and "
                    + "bf:authorizedAccessPoint: " + key);
        }
        
        return key;
    }
    
}
