package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfAnnotationConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfAnnotationConverter.class);
  
    
    public BfAnnotationConverter(String localNamespace) {      
        super(localNamespace );
    }
    
    @Override
    protected Model convert() {
        
        // TODO FIX
//        Resource relatedWork = linkingStatement.getResource();
//        outputModel.add(
//                relatedWork, Ld4lProperty.HAS_ANNOTATION.property(), subject);

        convertAnnotationBody();
        
        // TODO Add motivations based on linkingStatement predicate
        // addMotivation();
        
        // TODO: range of bf:assertionDate is a Literal, range of 
        // oa:annotatedAt is a xsd:dateTimeStamp. Need to convert Literal
        // text to xsd:dateTime format.
        // convertAnnotationDate();
        
        // TODO handle CoverArt, TableOfContents

        return super.convert();
    }
    
    private void convertAnnotationBody() {
        
        // only if linking stmt 
        Statement bodyStmt = 
                subject.getProperty(BfProperty.BF_ANNOTATION_BODY.property());
        if (bodyStmt == null) {
            return;
        }
        
        Resource body = bodyStmt.getResource();
        // retractions.add(bodyStmt);
        outputModel.add(body, RDF.type, Ld4lType.TEXT_CONTENT.ontClass());
        // It's not clear what predicate in Bibframe links the body Resource
        // to the text content, so can't at this point find the content of the
        // annotation.
        // outputModel.add(body, Ld4lProperty.CHARS.property(), "");
        
        // TODO
        // coverArt => hasCoverArt - need to call from Instance converter too

        // TODO Not tested - need to find relevant data in Bibframe RDF.
    }
    
}
