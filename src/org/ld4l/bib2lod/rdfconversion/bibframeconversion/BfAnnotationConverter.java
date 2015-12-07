package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

public class BfAnnotationConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfAnnotationConverter.class);
  

    private static final List<BfType> TYPES_TO_CONVERT = 
            new ArrayList<BfType>();
    static {
        TYPES_TO_CONVERT.add(BfType.BF_ANNOTATION);
    }
    
    private static final List<BfType> TYPES_TO_RETRACT = 
            new ArrayList<BfType>();
    static {
        
    }
    
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_ANNOTATES);
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
        assertions.add(
                relatedWork, Ld4lProperty.HAS_ANNOTATION.property(), subject);
        super.convertModel();
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
