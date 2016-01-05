package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.List;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.util.ResourceUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

public class BfLanguageConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfLanguageConverter.class);
    
    
    public BfLanguageConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    @Override
    protected Model convertModel() {
        convertOriginalLanguage();
        assignExternalUri();
        return super.convertModel();  
    }

    /**
     * Change the local URI assigned by the LC converter to the related external 
     * URI, linked by the converter via the property bf:languageOfPartUri.
     * The external URIs are part of the LC languages vocabulary; e.g.,
     * http://id.loc.gov/vocabulary/languages/ger.
     * NB In future we want to switch to the Lingvo language vocabulary.
     */
    private void assignExternalUri() {
        
        Property property = BfProperty.BF_LANGUAGE_OF_PART_URI.property();
        Resource object = subject.getPropertyResourceValue(property);
        if (object != null) {
            subject = ResourceUtils.renameResource(subject, object.getURI());
            subject.removeAll(property);
        }

    }
        
    private void convertOriginalLanguage() {
      
        Property resourcePartProp = BfProperty.BF_RESOURCE_PART.property();
        Statement resourcePartStmt = subject.getProperty(resourcePartProp);
        
        if (resourcePartStmt != null) {  
            String value = resourcePartStmt.getString();
            
            if (value.equals("original")) {                   
                Property langProp = BfProperty.BF_LANGUAGE.property();
                List<Statement> langStmts = inputModel.listStatements(              
                        null, langProp, subject).toList();
                
                // There should be only one statement, since each local language 
                // URI is unique to the Work.
                for (Statement langStmt : langStmts) {
                    Resource work = langStmt.getSubject();
                    Property originalLangProp = 
                            Ld4lProperty.HAS_ORIGINAL_LANGUAGE.property();
                    // OK to alter model here, since we're not continuing the 
                    // iteration.
                    // NB Must add to model rather than outputModel model, since 
                    // subject's URI may need to be changed in 
                    // assignExternalUri().
                    outputModel.add(work, originalLangProp, subject);
                    retractions.add(langStmt);
                }      
                
            } else {
                // Not sure what values other than "original" could occur; for 
                // now log them and remove them.  
                LOGGER.debug("Found value of bf:resourcePart " + value);
            }
            retractions.add(resourcePartStmt);
        }
        
        applyRetractions(inputModel, retractions);
    }
    
}
