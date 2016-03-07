package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lIndividual;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class BfAnnotationConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfAnnotationConverter.class);
    
    public BfAnnotationConverter(String localNamespace) {      
        super(localNamespace );
    }
    
    @Override
    protected Model convert() {
              
        convertAnnotationSubType();
        convertAnnotationBody();

        // TODO handle CoverArt, TableOfContents - but absent from current data

        return super.convert();
    }
    
    private void convertAnnotationSubType() {
        
        Resource type = subject.getPropertyResourceValue(RDF.type);
        if (type.equals(BfType.BF_ANNOTATION.type())) {
            return;
        }
        
        // bf:Review and bf:Summary
        // Assign general type ld4l:Annotation and represent Bibframe subtypes
        // with motivations.
        // Note: wherever the predicate linking the annotation to the work is
        // either bf:reviewOf or bf:summaryOf, the type bf:Review or bf:Summary,
        // respectively, is also assigned, so we don't need to look for the
        // predicate.
        Resource motivation;
        if (type.equals(BfType.BF_REVIEW.type())) {
            motivation = Ld4lIndividual.MOTIVATION_REVIEWING.individual();
        } else if (type.equals(BfType.BF_SUMMARY.type())) {
            motivation = Ld4lIndividual.MOTIVATION_SUMMARIZING.individual();
        } else {
            return;
        }

        outputModel.add(
                subject, Ld4lProperty.MOTIVATED_BY.property(), motivation); 
        
        // Handled in super.convert():
        // oa:Annotation type assertion 
        // Conversion of bf:summaryOf / bf:reviewOf to oa:hasTarget 
    }
    
    private void convertAnnotationBody() {
        
        Statement bodyStmt = 
                subject.getProperty(BfProperty.BF_ANNOTATION_BODY.property());
        
        if (bodyStmt == null) {
            return;
        }
 
        RDFNode body = bodyStmt.getObject();
        
        // If body is a resource, do nothing
        // Examples:
        // http://www.loc.gov/hlas/
        // http://muco.alexanderstreet.com/search/publishedStart/1874/publishedEnd/1877/sortby/published/composer/Mendelssohn%20Felix/publisher/Breitkopf%20and%20H%C3%A4rtel/tab/score
        if (body.isResource()) {
            return;
        }
        
        // If body is a literal, create a new body resource, and convert the
        // string to the body content.
        Resource bodyResource = outputModel.createResource(
                RdfProcessor.mintUri(localNamespace));
        outputModel.add(bodyResource, RDF.type, 
                    Ld4lType.TEXT_CONTENT.type());
        outputModel.add(subject, Ld4lProperty.HAS_ANNOTATION_BODY.property(), bodyResource);
        outputModel.add(bodyResource, Ld4lProperty.CHARS.property(), body);
    }
    
}
