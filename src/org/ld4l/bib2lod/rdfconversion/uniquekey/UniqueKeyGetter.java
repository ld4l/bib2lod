package org.ld4l.bib2lod.rdfconversion.uniquekey;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.query.ResultSet;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.bibframeconversion.BfInstanceConverter;
import org.ld4l.bib2lod.util.NacoNormalizer;

public class UniqueKeyGetter {
    
    private static final Logger LOGGER = 
            LogManager.getLogger(UniqueKeyGetter.class);

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
        
        
    // Using LinkedHashMap in case order of processing becomes important
    private static final Map<Resource, BfType> TYPES_FOR_BF_ONT_CLASSES =
            BfType.typesForOntClasses();

    private final Resource resource;

    public UniqueKeyGetter(Resource resource) {
        this.resource = resource;
    }

    /* Get the identifying key based on statements in which the resource is 
     * either the subject or object.
     */
    public String getKey() {
        
        String key = null;
        
        key = getKeyFromType();
        
        // Not sure yet if we need this
        if (key == null) {
            key = getKeyFromPredicate();
        }

        // TODO Doesn't apply to bf:Instance. Either test for that or move into
        // specific methods; or never send back null from the Instance method.
        // (The latter approach may be error-prone.)
        if (key == null) {
            key = getKeyFromBfLabel();
        }
        
        if (key == null) {
            // TEMPORARY! - or will this be the fallback?
            key = resource.getLocalName();
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
    

    private String getKeyFromBfLabel() {
        
        String bfLabel = null;

        List<Statement> statements = resource.listProperties(
                BfProperty.BF_LABEL.property()).toList();

        // If there is more than one (which there shouldn't be), we'll get the
        // first one, but doesn't matter if we have no selection criteria. 
        for (Statement statement : statements) {
            RDFNode object = statement.getObject();
            if (object.isLiteral()) {
                bfLabel = object.asLiteral().getLexicalForm();
                break;
            }
        }
    
        bfLabel = NacoNormalizer.normalize(bfLabel);
        LOGGER.debug("Got bf:label key " + bfLabel + " for resource " 
                + resource.getURI());
                
        return bfLabel;
    }
    

    private String getKeyFromType() {
        
        String key = null;

        // Get the BfTypes for this resource
        List<Statement> typeStmts = resource.listProperties(RDF.type).toList();
        List<BfType> types = new ArrayList<BfType>();
        for (Statement stmt : typeStmts) {
            Resource ontClass = stmt.getResource();
            types.add(TYPES_FOR_BF_ONT_CLASSES.get(ontClass));
        }
        
        // Could turn into subclasses - but there would be only one method, the
        // getKey method. Is there another more elegant way to do this?
        // We can't use a map, since entities may have multiple types, and we
        // use the if-statement to specify the relevant type.

        if (types.contains(BfType.BF_TOPIC)) {
            key = getBfAuthorityKey();
        } else if (BfType.isAuthority(types)) {  
            key = getBfAuthorityKey();
        } else if (types.contains(BfType.BF_EVENT)) {
            key = getBfEventKey();
        } else if (types.contains(BfType.BF_HELD_ITEM)) {
            key = getBfHeldItemKey();
        } else if (types.contains(BfType.BF_INSTANCE)) {
            key = getBfInstanceKey();
        } else if (types.contains(BfType.BF_WORK)) {
            key = getBfWorkKey();
        } else if (types.contains(BfType.BF_IDENTIFIER)) {
            key = getBfIdentifierKey();
        } else if (types.contains(BfType.BF_ANNOTATION)) {
            key = getBfAnnotationKey();
        } else if (types.contains(BfType.MADSRDF_AUTHORITY)) {
            key = getMadsAuthorityKey();
        } else if (types.contains(BfType.BF_CLASSIFICATION)) {
            key = getBfClassificationKey();
        } else if (types.contains(BfType.BF_LANGUAGE)) {
            key = getBfLanguageKey();
        } else if (types.contains(BfType.BF_TITLE)) {
            key = getBfTitleKey();          
        } else if (types.contains(BfType.BF_CATEGORY)) {
            key = getBfCategoryKey();  
        } else if (types.contains(BfType.BF_PROVIDER)) {
            key = getBfProviderKey();
        } // TODO add other types as needed.
               
        return key; 
    }

    private String getBfProviderKey() {
        // TODO Auto-generated method stub
        return null;
    }

    private String getBfLanguageKey() {
        // TODO Auto-generated method stub
        return null;
    }

    private String getBfClassificationKey() {
        // TODO Auto-generated method stub
        return null;
    }

    private String getBfAnnotationKey() {
        // TODO Auto-generated method stub
        return null;
    }

    private String getBfIdentifierKey() {
        // TODO Auto-generated method stub
        return null;
    }
  
    private String getBfAuthorityKey() {
 
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
    
    private String getBfEventKey() {

        // The only data we have for a bf:Event is bf:eventDate and 
        // bf:eventPlace. This isn't enough to reconcile resources; so 
        // generate only unique Event URIs.
        return resource.getLocalName();
    }
    
    private String getBfHeldItemKey() {
        String key = null;
        return key;
    }
    
    private String getBfInstanceKey() {
        String key = null;
        return key;
    }
    
    private String getBfWorkKey() {
        
        String authAccessPoint = null;
        
        List<Statement> statements = resource.listProperties(
                BfProperty.BF_AUTHORIZED_ACCESS_POINT.property()).toList();
        
        for (Statement s : statements) {
            RDFNode object = s.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                authAccessPoint = literal.getLexicalForm();
                String lang = literal.getLanguage();
                if (lang.equals("x-bf-hash")) {
                    LOGGER.debug("Got authAccessPoint key " + authAccessPoint
                            + " for resource " + resource.getURI());
                }
                // No need to look further, and no need to normalize. 
                // In fact, in a large data sample there are no Works without
                // an authorizedAccessPoint value with lang 'x-bf-hash'.
                return authAccessPoint;
            } 
        }
           
        return NacoNormalizer.normalize(authAccessPoint);
    }
    
    private String getBfTitleKey() {
        String key = null;
        return key;
    }
    
    /*
     * Get key for a madsrdf:Authority from the madsrdf:authoritativeLabel
     * property. Note that other resources that get their key from the
     * property chain bf:hasAuthority/madsrdf:authoritativeLabel are handled
     * in getKeyFromAuthoritativeLabel.
     */
    private String getMadsAuthorityKey() {
        
        String authoritativeLabel = null;

        List<Statement> statements = resource.listProperties(
                BfProperty.MADSRDF_AUTHORITATIVE_LABEL.property()).toList();

        for (Statement statement : statements) {
            RDFNode object = statement.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                authoritativeLabel = literal.getLexicalForm();
                break;
            }
        }
    
        // Prefix an additional string to the label, else the local name of 
        // the Authority will collide with that of the related resource, since
        // generally the madsrdf:authorizedLabel of the Authority and the
        // bf:authorizedAccessPoint of the related resource are identical.
        // *** TODO Are there other blank nodes that also need to be 
        // distinguished from the related resources?
        authoritativeLabel = "authority "  
                + NacoNormalizer.normalize(authoritativeLabel);
        LOGGER.debug("Got authoritativeLabel key " + authoritativeLabel
                + " for resource " + resource.getURI());
        
        return authoritativeLabel;
    }
    
    private String getBfCategoryKey() {
        String key = null;
        return key;
    }
    
    private String getKeyFromPredicate() {
        String key = null;
        return key;
    }

    private Resource inferTypeFromPredicates() {     
        // TODO Auto-generated method stub
        return null;
    }
 
}
