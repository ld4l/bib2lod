package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.StmtIterator;
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
    protected void convert() {
        convertConferenceName();
        super.convert();        
    }
   
    
    private void convertConferenceName() {

        StmtIterator stmts = model.listStatements(null, RDF.type, 
                BfType.MADSRDF_CONFERENCE_NAME.ontClass());
        if (stmts.hasNext()) { 
            // Add to assertions model rather than main model, so the
            // statement doesn't get reprocessed.
            assertions.add(subject, RDF.type, Ld4lType.CONFERENCE.ontClass());
            // Not retracting the original statement or any other statements
            // about the madsrdf:Authority.
        }

    }
  
}
