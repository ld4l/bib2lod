package org.ld4l.bib2lod.rdfconversion.uniqueuris;

import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.util.NacoNormalizer;

public class BfWorkUriGenerator extends BfResourceUriGenerator {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfWorkUriGenerator.class);

  
    public BfWorkUriGenerator(String localNamespace) {
        super(localNamespace);
    }
       
    @Override
    protected String getUniqueKey() {
        
        // All Cornell works and most Stanford and Harvard works have 
        // bf:authorizedAccessPoint
        String key = getKeyFromAuthorizedAccessPoint();
        
        // If no bf:authorizedAccessPoint, they usually have a bf:title
        if (key == null) {
            key = getKeyFromTitle();
        }
        
        // TODO Do we ever need to use the bf:Title object and its properties?
        
        // Fall back to bf:label
        if (key == null) {
            key = getKeyFromBfLabel();
        }
        
        // Final fallback to existing local name
        if (key == null) {
            key = resource.getLocalName();
        }
        
        return key;
    }
    
    private String getKeyFromAuthorizedAccessPoint() {

        String key = null;
        
        List<Statement> statements = resource.listProperties(
                BfProperty.BF_AUTHORIZED_ACCESS_POINT.property()).toList();
        
        for (Statement s : statements) {
            RDFNode object = s.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                key = literal.getLexicalForm();
                String lang = literal.getLanguage();
                if (lang.equals("x-bf-hash")) {
                    LOGGER.debug("Got authAccessPoint key " + key
                            + " for resource " + resource.getURI());
                }
                // No need to look further, and no need to normalize. 
                // In fact, in a large data sample there are no Works without
                // an authorizedAccessPoint value with lang 'x-bf-hash'.
                return key;
            } 
        }
                   
        return NacoNormalizer.normalize(key);        
    }
    
    private String getKeyFromTitle() {
        
        String key = null;

        List<Statement> statements = resource.listProperties(
                BfProperty.BF_TITLE.property()).toList();
        
        for (Statement s : statements) {
            RDFNode object = s.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                key = literal.getLexicalForm();
                break;
            } 
        }
                   
        return NacoNormalizer.normalize(key); 
    }
}
