package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);
    
    protected OntType type;
    
    public BfResourceConverter(OntType type) {
        LOGGER.trace("In constructor for " + this.getClass().getName());
        this.type = type;
        LOGGER.trace("Type for this converter is " + type);
    }
    
    public Model convert(Model inputModel) {  
        return inputModel;
    }
    
    // For development/debuggin
    protected void printModel(Model model) {
        StmtIterator statements = model.listStatements();
        while (statements.hasNext()) {
            Statement s = statements.nextStatement();
            LOGGER.debug(s.toString());
        }
    }

}
