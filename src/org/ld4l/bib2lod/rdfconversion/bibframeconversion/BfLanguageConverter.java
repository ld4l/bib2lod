package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfLanguageConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfLanguageConverter.class);
    
    private static final Ld4lType NEW_TYPE = Ld4lType.LANGUAGE;
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LANGUAGE, Ld4lProperty.LANGUAGE);
    }

//    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
//            Arrays.asList(
//                    BfProperty.BF_LANGUAGE_OF_PART_URI
//            );

    
    @Override
    protected void convert() {
        assignType(); 
        convertProperties();
        retractProperties();  
        assignExternalUri();
        changePropertyNamespaces();   
        LOGGER.debug(model);
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

        LOGGER.debug(model);
    }
    
    @Override
    protected void convertProperties() {
        convertOriginalLanguage();
        super.convertProperties();
    }
    
    private void convertOriginalLanguage() {
        
        Property resourcePartProp = BfProperty.BF_RESOURCE_PART.property();
        Statement resourcePartStmt = subject.getProperty(resourcePartProp);
        if (resourcePartStmt != null) {  
            if (resourcePartStmt.getLiteral().getLexicalForm()
                    .equals("original")) {
                Property langProp = BfProperty.BF_LANGUAGE.property();
                StmtIterator langStmts = 
                        model.listStatements(null, langProp, subject);
                // There should be only one, since each local language URI is
                // unique to the Work.
                if (langStmts.hasNext()) {
                    Statement langStmt = langStmts.nextStatement();
                    Resource work = langStmt.getSubject();
                    Property originalLangProp = 
                            Ld4lProperty.ORIGINAL_LANGUAGE.property();
                    // OK to alter model here, since we're not continuing the 
                    // iteration
                    model.add(work, originalLangProp, subject);
                    model.remove(langStmt);
                }      
            }
            model.remove(resourcePartStmt);
        }
    }
    
    @Override
    protected Ld4lType getNewType() {
        return NEW_TYPE;
    }

    @Override
    protected Map<BfProperty, Ld4lProperty> getPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected List<BfProperty> getPropertiesToRetract() {
        return null; //PROPERTIES_TO_RETRACT;
    }
    
}
