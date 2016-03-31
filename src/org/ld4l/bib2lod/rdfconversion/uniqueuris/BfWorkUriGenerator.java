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
        
        // All Cornell works and most Stanford and Harvard works have hashable
        // bf:authorizedAccessPoint
        String key = getKeyFromHashableAuthorizedAccessPoint();

        // Fall back to existing local name. It is not safe to use non-hashable
        // bf:authorizedAccessPoint, or bf:title or bf:label. That is, these are 
        // not sufficiently rich to uniquely identify a Work, and we would
        // reconcile URIs without enough data.
        // Some Works have madsrdf:authoritativeLabels, which may be sufficient
        // for unique identification, but according to the MADS ontology these
        // should only be assigned to madsrdf:Authority, so presumably a 
        // converter error that shouldn't be relied on.
        if (key == null) {
            key = super.getUniqueKey();
        }
        
        return key;
    }
    
    private String getKeyFromHashableAuthorizedAccessPoint() {

        List<Statement> statements = resource.listProperties(
                BfProperty.BF_AUTHORIZED_ACCESS_POINT.property()).toList();
        
        for (Statement s : statements) {
            RDFNode object = s.getObject();
            if (object.isLiteral()) {
                Literal literal = object.asLiteral();
                String key = literal.getLexicalForm();
                String lang = literal.getLanguage();
                if (lang.equals("x-bf-hash")) {
                    LOGGER.debug("Got authAccessPoint key " + key
                            + " for resource " + resource.getURI());
                    // No need to look further, and no need to normalize. 
                    // NB related works do not have this value.
                    return key;
                }
            } 
        }
                   
        return null;     
    }

}
