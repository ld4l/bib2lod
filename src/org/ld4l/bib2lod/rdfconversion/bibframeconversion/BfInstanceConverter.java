package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfInstanceConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfInstanceConverter.class);
    
 
    private static final Map<Resource, Resource> NEW_WORK_TYPES = 
            BfType.typeMap(Arrays.asList(
                    BfType.BF_COLLECTION,
                    BfType.BF_INTEGRATING,
                    BfType.BF_MONOGRAPH,
                    BfType.BF_MULTIPART_MONOGRAPH,
                    BfType.BF_SERIAL
            ));
                        
    /*
     * This case is problematic, since, unlike the Work, we do not have links to
     * the Instance's Items here. Furthermore, how would we know which of the
     * Instance's Items are Manuscripts?
     */
    private static final Map<Resource, Resource> NEW_ITEM_TYPES =
            BfType.typeMap(Arrays.asList(
                    BfType.BF_MANUSCRIPT
            ));
   
    private static final List<BfType> TYPES_TO_RETRACT = 
            new ArrayList<BfType>();
    static {
            TYPES_TO_RETRACT.add(BfType.BF_ARCHIVAL);
    }
   
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {

    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_PROVIDER_STATEMENT);
    }

    public BfInstanceConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    @Override 
    protected void convertModel() {
        
        convertInstance();
        
//        convertIdentifiers(); - create BfIdentifierConverter instance
//        
//        convertTitles(); - create BfTitleConverter instance
        
        super.convertModel();
        

    }
        
    private void convertInstance() {
        
        StmtIterator stmts = subject.listProperties();
        while (stmts.hasNext()) {
            
            Statement statement = stmts.nextStatement();
            Property predicate = statement.getPredicate();
            
            if (predicate.equals(RDF.type)) {
                if (convertInstanceTypeToWorkType(statement.getResource())) {
                    stmts.remove();
                }
                
                // Handle conversion to Item type here, when we know how...
                
            // TODO Do we ever get a providerStatement statement instead of a
            // Provider object? If so, we need to parse the literal value to
            // create the Provision and its associated properties.
            } else if (predicate.equals(BfProperty.BF_PUBLICATION.property())) {
                convertProvider(statement.getResource());
                stmts.remove();
            }
        }
    }
   

    private boolean convertInstanceTypeToWorkType(Resource type) {

        if (NEW_WORK_TYPES.containsKey(type)) {
            
            // Get the associated Work
            Statement instanceOf = 
                    subject.getProperty(BfProperty.BF_INSTANCE_OF.property());
            Resource relatedWork = instanceOf.getResource();
            assertions.add(relatedWork, RDF.type, NEW_WORK_TYPES.get(type));
            return true;
        }
 
        return false;
    }
    
    // TODO Could also create a BfProviderConverter, but since it only occurs 
    // with an Instance, may not be worth it. (Whereas Titles and Identifiers 
    // are related to multiple types of other Resources.) On the other hand,
    // would be a cleaner implementation.
    private void convertProvider(Resource subject) {

        StmtIterator stmts = subject.listProperties();
        while (stmts.hasNext()) {
            
            Statement statement = stmts.nextStatement();
            Property predicate = statement.getPredicate();
          
            if (predicate.equals(RDF.type)) {
                assertions.add(subject, RDF.type, 
                        Ld4lType.PUBLISHER_PROVISION.ontClass());
                assertions.add(subject, RDFS.label, "Publisher");
                
            } else if (predicate.equals(
                    BfProperty.BF_PROVIDER_NAME.property())) {
                assertions.add(subject, Ld4lProperty.AGENT.property(), 
                        statement.getResource());
                
            } else if (predicate.equals(
                    BfProperty.BF_PROVIDER_PLACE.property())) {
                assertions.add(subject, Ld4lProperty.AT_LOCATION.property(), 
                        statement.getResource());
            
            } else if (predicate.equals(
                    BfProperty.BF_PROVIDER_DATE.property())) {
                assertions.add(subject, Ld4lProperty.DATE.property(), 
                        statement.getLiteral());
            }
            
            stmts.remove();
        }
        assertions.add(
                this.subject, Ld4lProperty.HAS_PROVISION.property(), subject);

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
