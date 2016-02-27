package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfMeetingConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfMeetingConverter.class);

    
    public BfMeetingConverter(String localNamespace) {
        super(localNamespace);
    }
    
    
    @Override
    protected Model convert() {
        convertConferenceName();
        
        /* 
         * TODO ** What to do with Identifier, Authority, since we're changing
         * Meeting from an Authority to an Event?
         * Predicates of which Meetings are subjects:
         * bf:authoritySource
         * bf:authorizedAccessPoint
         * bf:hasAuthority
         * bf:systemNumber
         * Predicates of which Meetings are objects:
         * bf:contributor
         * bf:creator
         * bf:subject
         */
        
        
        
        return super.convert();        
    }
   
    
    private void convertConferenceName() {

        if (subject.hasProperty(
                RDF.type, BfType.MADSRDF_CONFERENCE_NAME.ontClass())) {
            outputModel.add(subject, RDF.type, Ld4lType.CONFERENCE.ontClass());
        }
    }

}
