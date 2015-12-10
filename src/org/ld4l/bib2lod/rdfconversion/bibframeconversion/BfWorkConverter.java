package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.BibframeConverter;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.OntNamespace;

public class BfWorkConverter extends BfBibResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfWorkConverter.class);
    
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_LANGUAGE);        
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_SUBJECT);
    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }
    
    private static final List<BfProperty> CONTRIBUTOR_PROPERTIES = 
            new ArrayList<BfProperty>();
    static {
        CONTRIBUTOR_PROPERTIES.add(BfProperty.BF_CONTRIBUTOR);
        CONTRIBUTOR_PROPERTIES.add(BfProperty.BF_CREATOR);
        CONTRIBUTOR_PROPERTIES.add(BfProperty.BF_RELATOR);
    }
 
    private static final List<BfProperty> ANNOTATION_TARGET_PROPERTIES =
            new ArrayList<BfProperty>();
    static {
        ANNOTATION_TARGET_PROPERTIES.add(BfProperty.BF_ANNOTATES);
        ANNOTATION_TARGET_PROPERTIES.add(BfProperty.BF_REVIEW_OF);
        ANNOTATION_TARGET_PROPERTIES.add(BfProperty.BF_SUMMARY_OF);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_AUTHORIZED_ACCESS_POINT);
        // LD4L uses the derivedFrom predicate to relate Titles to Titles and
        // Works to Works, but this is used to relate a Work or Instance to
        // a marcxml record, so it should be removed.
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_DERIVED_FROM);
    }

    public BfWorkConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }

    @Override 
    protected void convertModel() {
        
        convertTitles(BfProperty.BF_WORK_TITLE);

        // Copy statements to a list and loop through the list rather than
        // using the iterator. This allows us to modify the model inside the
        // loop.
        List<Statement> statements = model.listStatements().toList();     
        
        for (Statement statement : statements) {
            
            Property predicate = statement.getPredicate();
    
            if (predicate.equals(RDF.type)) {
                
                // needed, or fall through to superclass conversion?
              
            } else {
                
                BfProperty bfProp = BfProperty.get(predicate);
                
                if (bfProp == null) {
                    // Log for review, to make sure nothing has escaped.
                    LOGGER.debug("No specific handling defined for property " 
                            + predicate.getURI()
                            + "; falling through to default case.");
                    continue;
                }
                
                if (CONTRIBUTOR_PROPERTIES.contains(bfProp) || 
                        bfProp.namespace().equals(OntNamespace.RELATORS)) {
                    convertContributor(statement);
                    
                } else if (ANNOTATION_TARGET_PROPERTIES.contains(bfProp)) {
                    convertAnnotation(statement);
                    
                } 
            }          
        }
        //RdfProcessor.printModel(model, Level.DEBUG);
        super.convertModel();

    }
    
    private void convertContributor(Statement statement) {
        
        BfResourceConverter converter = new BfContributorConverter(
                this.localNamespace, statement);
        
        // Identify the provider resource and build its associated model (i.e.,
        // statements in which it is the subject or object).
        Resource contributor = BibframeConverter.getSubjectModelToConvert(
                statement.getResource());
        
        // Add BfContributorConverter model to this converter's assertions 
        // model, so they get added to the BibframeConverter output model.
        assertions.add(converter.convertSubject(contributor, statement));
        
        // TODO Is there any data in the Bibframe RDF that would allow 
        // assignment of vivo:rank to the Contributions?
    }
    
    private void convertAnnotation(Statement statement) {
        
        BfResourceConverter converter = 
                new BfAnnotationConverter(localNamespace, statement);

        // Identify the annotation resource and build its associated model (i.e.,
        // statements in which it is the subject or object).
        Resource annotation = BibframeConverter.getSubjectModelToConvert(
                statement.getSubject()); 
        
        // Add BfAnnotationConverter model to this converter's assertions 
        // model, so they get added to the BibframeConverter output model.
        assertions.add(converter.convertSubject(annotation, statement));        
        
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
