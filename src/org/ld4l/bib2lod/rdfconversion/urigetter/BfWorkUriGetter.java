package org.ld4l.bib2lod.rdfconversion.urigetter;

import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.util.NacoNormalizer;

public class BfWorkUriGetter extends ResourceUriGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfWorkUriGetter.class);
    
    public BfWorkUriGetter(Resource resource, String localNamespace) {
        super(resource, localNamespace);
    }

    @Override
    protected String getUniqueKey() {
        
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
}
