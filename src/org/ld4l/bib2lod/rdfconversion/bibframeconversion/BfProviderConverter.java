package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfProviderConverter extends BfResourceConverter {


    private static final Logger LOGGER = 
            LogManager.getLogger(BfProviderConverter.class);

    
    private static final List<BfType> TYPES_TO_CONVERT = 
            new ArrayList<BfType>();
    static {
 
    }
    
    private static final List<BfType> TYPES_TO_RETRACT = 
            new ArrayList<BfType>();
    static {
        TYPES_TO_RETRACT.add(BfType.BF_PROVIDER);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PROVIDER_DATE);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_DISTRIBUTION);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_MANUFACTURE);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PRODUCTION);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PROVIDER);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PROVIDER_NAME);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PROVIDER_PLACE);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PROVIDER_ROLE);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PUBLICATION);
    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {

    }
    
    private static final Map<BfProperty, Ld4lType> PROPERTY_TO_TYPE = 
            new HashMap<BfProperty, Ld4lType>();
    static {
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_DISTRIBUTION, Ld4lType.DISTRIBUTOR_PROVISION);
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_MANUFACTURE, Ld4lType.MANUFACTURER_PROVISION);
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_PRODUCTION, Ld4lType.PRODUCER_PROVISION);
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_PROVIDER, Ld4lType.PROVISION);
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_PUBLICATION, Ld4lType.PUBLISHER_PROVISION);
    }
    
    protected BfProviderConverter(String localNamespace, Statement statement) {
        super(localNamespace, statement);
    }   
    
    @Override
    protected void convertModel() {
        
      createProvision();
      
      // If there were retractions, we should apply them to the model here,
      // so that super.convertModel() doesn't reprocess them.
      
      super.convertModel();
    }

    private void createProvision() {
        
        Property providerProp = linkingStatement.getPredicate();
        LOGGER.debug(providerProp.getURI());
        Ld4lType newType = null;
        BfProperty bfProp = BfProperty.get(providerProp);
        
        newType = PROPERTY_TO_TYPE.containsKey(bfProp) ? 
                PROPERTY_TO_TYPE.get(bfProp) : Ld4lType.PROVISION;
   
        /*
         * TODO bf:providerRole
         * If bf:providerRole is specified, this should be replaced with a
         * specific Provision subtype. If it doesn't match any of the existing
         * subtypes, convert to a label on the generic ld4l:Provision. Need to
         * analyze what values, if any, are generated.
         *
         * TODO bf:providerStatement
         * For now will move to legacy namespace. If the statement exists 
         * without the provider and its object properties, should parse the 
         * providerStatement to construct these. If it occurs alongside the 
         * provider and object properties, and the data is the same, it should 
         * be discarded.
        */
        
        if (newType != null) {
            assertions.add(subject, RDF.type, newType.ontClass());
        }
        
        if (newType.label() != null) {
            assertions.add(subject, RDFS.label, newType.label());
        }


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
