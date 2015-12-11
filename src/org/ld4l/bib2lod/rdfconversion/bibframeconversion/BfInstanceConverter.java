package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.BibframeConverter;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfInstanceConverter extends BfBibResourceConverter {

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
     * the Instance's Items here. However, in the TypeSplitter we could create
     * the inverse assertions (Instance to Item) and add those to the Instance
     * file. The real problem is: if the Instance has more than one Item, how 
     * would we know which of the Instance's Items are Manuscripts? 
     private static final Map<Resource, Resource> NEW_ITEM_TYPES =
            BfType.typeMap(Arrays.asList(
                    BfType.BF_MANUSCRIPT
            ));
     */
    
    private static final List<BfType> TYPES_TO_RETRACT = 
            new ArrayList<BfType>();
    static {
            TYPES_TO_RETRACT.add(BfType.BF_ARCHIVAL);
    }

    private static final List<BfProperty> PROVIDER_PROPERTIES = 
            new ArrayList<BfProperty>();
    static {
        PROVIDER_PROPERTIES.add(BfProperty.BF_DISTRIBUTION);
        PROVIDER_PROPERTIES.add(BfProperty.BF_MANUFACTURE);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PRODUCTION);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PROVIDER);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PUBLICATION);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PROVIDER_STATEMENT);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_RELATED_INSTANCE);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_SUPPLEMENTARY_CONTENT_NOTE);

    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }

    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        // LD4L uses a derivedFrom predicate to relate Titles to Titles and
        // Works to Works, but this is used to relate a Work or Instance to
        // a marcxml record, so it should be removed.
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_DERIVED_FROM);
    }
    
    public BfInstanceConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    @Override 
    protected void convertModel() {
        
        // Get the associated Work
        Statement instanceOf = 
                subject.getProperty(BfProperty.BF_INSTANCE_OF.property());
        // Probably can't be null
        Resource relatedWork = 
                instanceOf != null ? instanceOf.getResource() : null;
        
        convertTitles(BfProperty.BF_INSTANCE_TITLE);

        // Copy statements to a list and loop through the list rather than
        // using the iterator. This allows modifying the model inside the
        // loop.
        
        //?????? SHouldn't this be subject.listProperties().toList()???
        // Why do we want to iterate through other stmts? BUT when I try this,
        // the other stmts get lost. Same issue in BfWorkConverter
        List<Statement> statements = model.listStatements().toList();
 
        for (Statement statement : statements) {
            
            Property predicate = statement.getPredicate();
    
            if (predicate.equals(RDF.type)) {
                if (convertInstanceTypeToWorkType(
                        statement.getResource(), relatedWork)) {
                    retractions.add(statement);
                }
                
                // Handle conversion to Item type here, when we know how...
              
            } else {
                
                BfProperty bfProp = BfProperty.get(predicate);
                
                if (bfProp == null) {
                    // Log for review, to make sure nothing has escaped.
                    LOGGER.debug("No specific handling defined for property " 
                            + predicate.getURI()
                            + "; falling through to default case.");
                    continue;
                }
                
                // TODO Do we ever get a providerStatement instead of a 
                // Provider object? If so, we need to parse the literal value 
                // to create the Provision and its associated properties (cf.
                // titleStatement).
                if (PROVIDER_PROPERTIES.contains(bfProp)) {
                    convertProvider(statement);
                    
                } else if (bfProp.equals(BfProperty.BF_IDENTIFIER) || 
                        bfProp.equals(BfProperty.BF_SYSTEM_NUMBER)) {
                    convertIdentifier(statement);
                                              
                } else if (bfProp.equals(BfProperty.BF_MODE_OF_ISSUANCE)) {
                    if (relatedWork != null) {
                        // Probably also added from conversion of statement
                        // :instance a Monograph
                        assertions.add(relatedWork, RDF.type, 
                                Ld4lType.MONOGRAPH.ontClass());
                        retractions.add(statement);
                    }                   
                }
            }          
        }
        
        super.convertModel();
    }
  
    private boolean convertInstanceTypeToWorkType(
            Resource type, Resource relatedWork) {

        if (NEW_WORK_TYPES.containsKey(type)) {
            if (relatedWork != null) {
                assertions.add(relatedWork, RDF.type, NEW_WORK_TYPES.get(type));
            }
            return true;
        }
 
        return false;
    }
    
    private void convertProvider(Statement statement) {
        
        BfResourceConverter converter = new BfProviderConverter(
                this.localNamespace, statement);
     
        // Identify the provider resource and build its associated model (i.e.,
        // statements in which it is the subject or object).
        Resource provider = BibframeConverter.getSubjectModelToConvert(
                statement.getResource());
        
        // Add BfProviderConverter model to this converter's assertions model,
        // so they get added to the BibframeConverter output model.
        assertions.add(converter.convertSubject(provider, statement));
    }
    
    private void convertIdentifier(Statement statement) {
        
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
