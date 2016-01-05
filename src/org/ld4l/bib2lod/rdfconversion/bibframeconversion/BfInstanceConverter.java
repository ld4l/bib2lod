package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
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
     * the inverse outputModel (Instance to Item) and add those to the Instance
     * file. The real problem is: if the Instance has more than one Item, how 
     * would we know which of the Instance's Items are Manuscripts? 
     private static final Map<Resource, Resource> NEW_ITEM_TYPES =
            BfType.typeMap(Arrays.asList(
                    BfType.BF_MANUSCRIPT
            ));
     */


    private static final List<BfProperty> PROVIDER_PROPERTIES = 
            new ArrayList<BfProperty>();
    static {
        PROVIDER_PROPERTIES.add(BfProperty.BF_DISTRIBUTION);
        PROVIDER_PROPERTIES.add(BfProperty.BF_MANUFACTURE);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PRODUCTION);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PROVIDER);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PUBLICATION);
    }
    
    public BfInstanceConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    @Override 
    protected Model convertModel() {
        
        LOGGER.debug("Converting instance " + subject.getURI());
        
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
        
        // Shouldn't this be subject.listProperties().toList()?
        // Why do we want to iterate through other stmts? BUT when I try this,
        // the other stmts get lost. Same issue in BfWorkConverter
        List<Statement> statements = inputModel.listStatements().toList();
 
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
                    LOGGER.debug("No handling defined for property " 
                            + predicate.getURI()
                            + "; deleting statement.");
                    continue;
                }
                
                // RDFNode object = statement.getObject();
                
                // TODO Do we ever get a providerStatement instead of a 
                // Provider object? If so, we need to parse the literal value 
                // to create the Provision and its associated properties (cf.
                // titleStatement).
                if (PROVIDER_PROPERTIES.contains(bfProp)) {
                    convertProvider(statement);
 
// TODO - Move to BfResourceConverter.convertModel()
//                } else if (bfProp.equals(BfProperty.BF_IDENTIFIER) || 
//                        bfProp.equals(BfProperty.BF_SYSTEM_NUMBER)) {
//                    convertIdentifier(statement);
                                              
                } else if (bfProp.equals(BfProperty.BF_MODE_OF_ISSUANCE)) {
                    if (relatedWork != null) {
                        // Probably also added from conversion of statement
                        // :instance a Monograph
                        outputModel.add(relatedWork, RDF.type, 
                                Ld4lType.MONOGRAPH.ontClass());
                        retractions.add(statement);
                    }        
                                    
                }
            }          
        }

        return super.convertModel();
    }
  
    private boolean convertInstanceTypeToWorkType(
            Resource type, Resource relatedWork) {

        if (NEW_WORK_TYPES.containsKey(type)) {
            if (relatedWork != null) {
                outputModel.add(
                        relatedWork, RDF.type, NEW_WORK_TYPES.get(type));
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
        Resource provider = getSubjectModelToConvert(statement.getResource());
                      
        // Add BfProviderConverter model to this converter's outputModel model,
        // so they get added to the BibframeConverter output model.
        outputModel.add(converter.convert(provider));
    }

}
