package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

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
    
    private static final Map<BfProperty, Ld4lType> 
            CONTRIBUTOR_PROPERTY_TO_TYPE = new HashMap<BfProperty, Ld4lType>();
            
    static {
        CONTRIBUTOR_PROPERTY_TO_TYPE.put(
                BfProperty.BF_CREATOR, Ld4lType.CREATOR_CONTRIBUTION);
        CONTRIBUTOR_PROPERTY_TO_TYPE.put(
                BfProperty.BF_CONTRIBUTOR, Ld4lType.CONTRIBUTION);
        CONTRIBUTOR_PROPERTY_TO_TYPE.put(
                BfProperty.BF_RELATOR, Ld4lType.CONTRIBUTION);
        CONTRIBUTOR_PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_AUTHOR, Ld4lType.AUTHOR_CONTRIBUTION);
        CONTRIBUTOR_PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_COMPOSER, Ld4lType.COMPOSER_CONTRIBUTION);
        CONTRIBUTOR_PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_CONDUCTOR, Ld4lType.CONDUCTOR_CONTRIBUTION);
        CONTRIBUTOR_PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_EDITOR, Ld4lType.EDITOR_CONTRIBUTION);
        CONTRIBUTOR_PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_NARRATOR, Ld4lType.NARRATOR_CONTRIBUTION);
        CONTRIBUTOR_PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_PERFORMER, Ld4lType.PERFORMER_CONTRIBUTION);
    }
 
//    private static final List<BfProperty> ANNOTATION_TARGET_PROPERTIES =
//            new ArrayList<BfProperty>();
//    static {
//        ANNOTATION_TARGET_PROPERTIES.add(BfProperty.BF_ANNOTATES);
//        ANNOTATION_TARGET_PROPERTIES.add(BfProperty.BF_REVIEW_OF);
//        ANNOTATION_TARGET_PROPERTIES.add(BfProperty.BF_SUMMARY_OF);
//    }

    public BfWorkConverter(String localNamespace) {
        super(localNamespace);
    }

    @Override 
    protected Model convert() {
        
        // convertTitles(BfProperty.BF_WORK_TITLE);

        StmtIterator statements = subject.getModel().listStatements();

        while (statements.hasNext()) {
            
            Statement statement = statements.nextStatement();
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();
            
            BfProperty bfProp = BfProperty.get(predicate);

            if (bfProp == null) {
                // Log for review, to make sure nothing has escaped.
                LOGGER.debug("No specific handling defined for property " 
                        + predicate.getURI()
                        + "; falling through to default case.");
                continue;
            }
            
            if (bfProp.equals(BfProperty.BF_LANGUAGE)) {
                convertLanguage(object);
            }
            
            else if (CONTRIBUTOR_PROPERTY_TO_TYPE.keySet().contains(bfProp)) {
                convertContributor(statement);
            }   
//                } else if (ANNOTATION_TARGET_PROPERTIES.contains(bfProp)) {
//                    convertAnnotation(statement);                   
//                }       
        }

        return super.convert();
    }
    
    private void convertLanguage(RDFNode language) {
        // Languages with local URIs are handled differently - see 
        // BfLanguageConverter. The languages in the external vocabulary 
        // handled here will not go to BfLanguageConverter, because they never
        // serve as subjects.
        if (language.isResource() 
                && language.asResource().getURI().startsWith(
                        Vocabulary.LANGUAGE.uri())) {
            outputModel.add(
                    subject, Ld4lProperty.HAS_LANGUAGE.property(), language);
        }
    }
    
    private void convertContributor(Statement statement) {
        
        BfProperty bfProp = BfProperty.get(statement.getPredicate());
        Ld4lType ld4lType = CONTRIBUTOR_PROPERTY_TO_TYPE.get(bfProp);
        Resource work = statement.getSubject();
        
        // Identify the provider resource and build its associated model (i.e.,
        // statements in which it is the subject or object).
        Resource agent = statement.getResource();
        
        Resource contribution = outputModel.createResource(
                RdfProcessor.mintUri(localNamespace));
        outputModel.add(contribution, RDF.type, ld4lType.ontClass());

        
        if (ld4lType.label() != null) {
            outputModel.add(contribution, RDFS.label, ld4lType.label());
        }
        
        outputModel.add(contribution, Ld4lProperty.HAS_AGENT.property(), agent);
        
        outputModel.add(
                work, Ld4lProperty.HAS_CONTRIBUTION.property(), contribution);
        
        // TODO Is there any data in the Bibframe RDF that would allow 
        // assignment of vivo:rank to the Contributions?
    }
    
//    private void convertAnnotation(Statement statement) {
//        
//        BfResourceConverter converter = 
//                new BfAnnotationConverter(localNamespace);
//
//        // Identify the annotation resource and build its associated model 
//        // (i.e., statements in which it is the subject or object).
//        Resource annotation = statement.getSubject();
//        Model annotationModel = getResourceSubModel(annotation);
//        Resource newAnnotation = 
//                annotationModel.createResource(annotation.getURI());
//                        
//        // Add BfAnnotationConverter model to this converter's outputModel 
//        // model, so they get added to the BibframeConverter output model.
//        Model convertedModel = converter.convert(newAnnotation);
//		outputModel.add(convertedModel);
//		convertedModel.close();
//    }
    
}
