package org.ld4l.bib2lod.rdfconversion.typededuping;

import java.util.List;
import java.util.Map;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntType;

// TODO If only abstract methods, change to an interface
public abstract class TypeDeduper {

    private static final Logger LOGGER = 
            LogManager.getLogger(TypeDeduper.class);
    
    protected Model newStatements = ModelFactory.createDefaultModel();
            
    public abstract Map<String, String> dedupe(OntType type, Model model);
      
    protected String getDefaultResourceKey(QuerySolution soln) {
        // NB It's assumed that bf:authorizedAccessPoint and bf:label values 
        // are identical when both exist. If that turns out to be wrong, we 
        // need some way of resolving discrepancies.

        // Return authorizedAccessPoint value, if there is one
        Literal authAccessPoint = soln.getLiteral("authAccessPoint");
        if (authAccessPoint != null) {
            LOGGER.debug("Using bf:authorizedAccessPoint to dedupe");
            return authAccessPoint.getLexicalForm();
        }
        
        // Else return bf:label, if there is one
        Literal label = soln.getLiteral("label");
        if (label != null) {
            LOGGER.debug("Using bf:label to dedupe");
            return label.getLexicalForm();
        }
        
        return null;  
    }
    
    public Model getNewStatements() {
        return newStatements;
    }

}
