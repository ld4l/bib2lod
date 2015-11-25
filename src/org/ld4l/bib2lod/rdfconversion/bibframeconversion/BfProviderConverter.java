package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
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
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PROVIDER_NAME);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PROVIDER_PLACE);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PROVIDER_STATEMENT);
    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_PROVIDER_ROLE);
    }

    private Statement providerStatement;

    public BfProviderConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    @Override
    protected Model convertSubject(Resource subject, Statement statement) {
        
        this.subject = subject;
        this.model = subject.getModel();
        this.providerStatement = statement;
        
        convertModel();
        
        model.add(assertions)
             .remove(retractions);
        
        return model;        
    }
    
    @Override
    protected void convertModel() {
        
      createProvision();
      model.remove(providerStatement);
      
      super.convertModel();
    }

    private void createProvision() {
        
        Resource relatedInstance = providerStatement.getSubject();
        Property providerProp = providerStatement.getPredicate();
        
        assertions.add(relatedInstance, Ld4lProperty.HAS_PROVISION.property(), 
                subject);
        
        Ld4lType newType = null;
        String label = null;
        
        // TODO Put these into some kind of data structure...
        if (providerProp.equals(BfProperty.BF_PUBLICATION.property())) {
            newType = Ld4lType.PUBLISHER_PROVISION;
            label = "Publisher";
        
        } else if (providerProp.equals(BfProperty.BF_DISTRIBUTION.property())) {
            newType = Ld4lType.DISTRIBUTOR_PROVISION;
            label = "Distributor";           

        } else if (providerProp.equals(BfProperty.BF_MANUFACTURE.property())) {
            newType = Ld4lType.MANUFACTURER_PROVISION;
            label = "Manufacturer";       

        } else if (providerProp.equals(BfProperty.BF_PRODUCTION.property())) {
            newType = Ld4lType.PRODUCER_PROVISION;
            label = "Producer";       
    
        // add any additional Provision subtypes here
    
        // Generic provider property
        } else {
            newType = Ld4lType.PROVISION;
            label = "Provider";
        }
        

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
        
        if (label != null) {
            assertions.add(subject, RDFS.label, label);
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
