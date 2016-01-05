package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfMeetingConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfMeetingConverter.class);

    
    public BfMeetingConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    
    @Override
    protected Model convertModel() {
        convertConferenceName();
        
        // TODO ** What to do with Identifier, Authority, since we're changing
        // Meeting from an Authority to an Event?
        
        return super.convertModel();        
    }
   
    
    private void convertConferenceName() {

        List<Statement> stmts = inputModel.listStatements(null, RDF.type, 
                BfType.MADSRDF_CONFERENCE_NAME.ontClass()).toList();
        for (Statement stmt : stmts) { 
            outputModel.add(subject, RDF.type, Ld4lType.CONFERENCE.ontClass());
            // Not retracting the original statement or any other statements
            // about the madsrdf:Authority.
            retractions.add(stmt);
        }

    }


}
