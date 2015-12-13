package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfAnnotationConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfAnnotationConverter.class);
  

    private static final List<BfType> TYPES_TO_CONVERT = 
            new ArrayList<BfType>();
    static {
        TYPES_TO_CONVERT.add(BfType.BF_ANNOTATION);
        TYPES_TO_CONVERT.add(BfType.BF_REVIEW);
        TYPES_TO_CONVERT.add(BfType.BF_SUMMARY);
    }
    
    private static final List<BfType> TYPES_TO_RETRACT = 
            new ArrayList<BfType>();
    static {
        
    }
    
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_ANNOTATES);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_ANNOTATION_ASSERTED_BY);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_ANNOTATION_BODY);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_ANNOTATION_SOURCE);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_ASSERTION_DATE); 
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_REVIEW);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_REVIEW_OF);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_SUMMARY);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_SUMMARY_OF);
    }
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_CHANGE_DATE);
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_DERIVED_FROM);
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_DESCRIPTION_CONVENTIONS);
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_DESCRIPTION_MODIFIER);
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_DESCRIPTION_SOURCE);
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_GENERATION_PROCESS);
    }
    protected BfAnnotationConverter(
            String localNamespace, Statement statement) {
        super(localNamespace, statement);
    }
    
    @Override
    protected void convertModel() {
        
        Resource relatedWork = linkingStatement.getResource();
        outputModel.add(
                relatedWork, Ld4lProperty.HAS_ANNOTATION.property(), subject);

        convertAnnotationBody();
        
        // TODO Add motivations based on linkingStatement predicate
        // addMotivation();
        
        // TODO: range of bf:assertionDate is a Literal, range of 
        // oa:annotatedAt is a xsd:dateTimeStamp. Need to convert Literal
        // text to xsd:dateTime format.
        // convertAnnotationDate();
        
        // TODO handle CoverArt, TableOfContents

        super.convertModel();
    }
    
    private void convertAnnotationBody() {
        
        // only if linking stmt 
        Statement bodyStmt = 
                subject.getProperty(BfProperty.BF_ANNOTATION_BODY.property());
        if (bodyStmt == null) {
            return;
        }
        
        Resource body = bodyStmt.getResource();
        retractions.add(bodyStmt);
        outputModel.add(body, RDF.type, Ld4lType.TEXT_CONTENT.ontClass());
        // It's not clear what predicate in Bibframe links the body Resource
        // to the text content, so can't at this point find the content of the
        // annotation.
        // outputModel.add(body, Ld4lProperty.CHARS.property(), "");
        
        // TODO
        // coverArt => hasCoverArt - need to call from Instance converter too

        // TODO Not tested - need to find relevant data in Bibframe RDF.
    }

    @Override 
    protected List<BfType> getBfTypesToConvert() {
        return TYPES_TO_CONVERT;
    }
    
    @Override 
    protected List<BfType> getBfTypesToRetract() {
        return TYPES_TO_RETRACT;
    }
    
    @Override
    protected List<BfProperty> getBfPropertiesToConvert() {
        return PROPERTIES_TO_CONVERT;
    }
   
    @Override
    protected Map<BfProperty, Ld4lProperty> getBfPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected List<BfProperty> getBfPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }
}
