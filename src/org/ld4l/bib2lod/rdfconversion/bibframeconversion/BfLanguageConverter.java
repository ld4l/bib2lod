package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class BfLanguageConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfLanguageConverter.class);
    

    private static final Map<Property, Property> PROPERTY_MAP = 
            new HashMap<Property, Property>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LANGUAGE_OF_PART.property(), 
                Ld4lProperty.LABEL.property());
    }
 
    
    @Override
    protected void convert() {
        convertOriginalLanguage();
        assignExternalUri();
        super.convert();  
    }

    /**
     * Change the local URI assigned by the LC converter to the related external 
     * URI, linked by the converter via the property bf:languageOfPartUri.
     * The external URIs are part of the LC languages vocabulary; e.g.,
     * http://id.loc.gov/vocabulary/languages/ger.
     * NB In future we want to switch to the Lingvo language vocabulary.
     */
    private void assignExternalUri() {
        
        LOGGER.debug("In assignExternalUri()");
        RdfProcessor.printModel(model, Level.DEBUG);
        LOGGER.debug(subject.getURI());
        Property property = BfProperty.BF_LANGUAGE_OF_PART_URI.property();
        Resource object = subject.getPropertyResourceValue(property);
        if (object != null) {
            LOGGER.debug(object.getURI());
            subject = ResourceUtils.renameResource(subject, object.getURI());
            subject.removeAll(property);
        }

        LOGGER.debug("At end of assignExternalUri()");
        RdfProcessor.printModel(model, Level.DEBUG);
    }
    
    
    private void convertOriginalLanguage() {
        
        LOGGER.debug("In convertOriginalLanguage");
        RdfProcessor.printModel(model, Level.DEBUG);        
        Property resourcePartProp = BfProperty.BF_RESOURCE_PART.property();
        Statement resourcePartStmt = subject.getProperty(resourcePartProp);
        if (resourcePartStmt != null) {  
            String value = resourcePartStmt.getString();
            if (value.equals("original")) {                   
                Property langProp = BfProperty.BF_LANGUAGE.property();
                StmtIterator langStmts = 
                        model.listStatements(null, langProp, subject);
                // There should be only one statement, since each local language 
                // URI is unique to the Work.
                if (langStmts.hasNext()) {
                    Statement langStmt = langStmts.nextStatement();
                    Resource work = langStmt.getSubject();
                    Property originalLangProp = 
                            Ld4lProperty.HAS_ORIGINAL_LANGUAGE.property();
                    // OK to alter model here, since we're not continuing the 
                    // iteration.
                    // NB Must add to model rather than assertions model, since 
                    // subject's URI may need to be changed in 
                    // assignExternalUri().
                    model.add(work, originalLangProp, subject);
                    model.remove(langStmt);
                }      
            } else {
                // Not sure what values other than "original" could occur; for 
                // now log them and remove them.  
                LOGGER.info("Found value of bf:resourcePart " + value);
            }
            model.remove(resourcePartStmt);
        }
    }
    

    @Override
    protected Map<Property, Property> getPropertyMap() {
        return PROPERTY_MAP;
    }

    
}
