package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.OntNamespace;

public class BfWorkConverter extends BfBibResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfWorkConverter.class);
 
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

    public BfWorkConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }

    @Override 
    protected Model convertModel() {
        
        convertTitles(BfProperty.BF_WORK_TITLE);

        // Copy statements to a list and loop through the list rather than
        // using the iterator. This allows us to modify the model inside the
        // loop.
        List<Statement> statements = inputModel.listStatements().toList();     
        
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

        return super.convertModel();

    }
    
    private void convertContributor(Statement statement) {
        
        BfResourceConverter converter = new BfContributorConverter(
                this.localNamespace, statement);
        
        // Identify the provider resource and build its associated model (i.e.,
        // statements in which it is the subject or object).
        Resource contributor = 
                getSubjectModelToConvert(statement.getResource());
                       
        // Add BfContributorConverter model to this converter's outputModel 
        // model, so they get added to the BibframeConverter output model.
        Model convert = converter.convert(contributor, statement);
		outputModel.add(convert);
		convert.close();
        
        // TODO Is there any data in the Bibframe RDF that would allow 
        // assignment of vivo:rank to the Contributions?
    }
    
    private void convertAnnotation(Statement statement) {
        
        BfResourceConverter converter = 
                new BfAnnotationConverter(localNamespace, statement);

        // Identify the annotation resource and build its associated model (i.e.,
        // statements in which it is the subject or object).
        Resource annotation = getSubjectModelToConvert(statement.getSubject());
                        
        // Add BfAnnotationConverter model to this converter's outputModel 
        // model, so they get added to the BibframeConverter output model.
        Model convert = converter.convert(annotation, statement);
		outputModel.add(convert);
		convert.close();
    }
    
}
