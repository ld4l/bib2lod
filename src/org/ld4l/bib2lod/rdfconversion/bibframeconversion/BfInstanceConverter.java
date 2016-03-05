package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfInstanceConverter extends BfBibResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfInstanceConverter.class);
    
    private static final Map<BfType, Ld4lType> WORK_TYPES = 
            new HashMap<BfType, Ld4lType>();
    static {
        WORK_TYPES.put(BfType.BF_COLLECTION, Ld4lType.COLLECTION);
        WORK_TYPES.put(BfType.BF_INTEGRATING, Ld4lType.INTEGRATING_RESOURCE);
        WORK_TYPES.put(BfType.BF_MONOGRAPH, Ld4lType.MONOGRAPH);
        WORK_TYPES.put(BfType.BF_MULTIPART_MONOGRAPH, 
                Ld4lType.MULTIPART_MONOGRAPH);
        WORK_TYPES.put(BfType.BF_SERIAL, Ld4lType.SERIAL);
    }
    private static final Map<Resource, Resource> WORK_TYPE_MAP = 
            BfType.typeMap(WORK_TYPES);

    private static final Map<BfType, Ld4lType> ITEM_TYPES = 
            new HashMap<BfType, Ld4lType>();
    static {
        ITEM_TYPES.put(BfType.BF_MANUSCRIPT, Ld4lType.MANUSCRIPT);
    }
    private static final Map<Resource, Resource> ITEM_TYPE_MAP = 
            BfType.typeMap(ITEM_TYPES);

    private static final List<BfProperty> PROVIDER_PROPERTIES = 
            new ArrayList<BfProperty>();
    static {
        PROVIDER_PROPERTIES.add(BfProperty.BF_DISTRIBUTION);
        PROVIDER_PROPERTIES.add(BfProperty.BF_MANUFACTURE);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PRODUCTION);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PROVIDER);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PUBLICATION);
    }
    
    public BfInstanceConverter(String localNamespace) {
        super(localNamespace);
    }
    
    @Override 
    protected Model convert() {
        
        LOGGER.debug("Converting instance " + subject.getURI());
        
        // *** TODO Create the sameAs to the worldcat instance here
        
        // Get the associated Work
        Statement instanceOf = 
                subject.getProperty(BfProperty.BF_INSTANCE_OF.property());
        // Probably can't be null
        Resource relatedWork = 
                instanceOf != null ? instanceOf.getResource() : null;
                               
        // Get any associated Items
        List<Statement> hasHolding = subject.getModel().listStatements(
                null, BfProperty.BF_HOLDING_FOR.property(), subject).toList();
        // Assign relatedItem only if there's just one. Otherwise, we don't
        // know which Item the transformations should apply to.
        Resource relatedItem = hasHolding.size() == 1 ? 
                hasHolding.get(0).getSubject() : null;
        
        // TODO Figure out whether to do here or within the StmtIterator
        convertTitles(BfProperty.BF_INSTANCE_TITLE);

        StmtIterator stmts = subject.getModel().listStatements();
 
        while (stmts.hasNext()) {
            Statement statement = stmts.nextStatement();            
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();
    
            if (predicate.equals(RDF.type)) {
                
                if (moveTypeToRelatedResource(
                        object.asResource(), relatedWork, WORK_TYPE_MAP)) {
                
                } else if (moveTypeToRelatedResource(
                        object.asResource(), relatedItem, ITEM_TYPE_MAP)) {
                    
                } // else default conversions handled in super.convert()
              
            } else {
                
                BfProperty bfProp = BfProperty.get(predicate);
                
                if (bfProp == null) {
                    // Log for review, to make sure nothing has escaped.
                    LOGGER.debug("No handling defined for property " 
                            + predicate.getURI()
                            + "; deleting statement.");
                    continue;
                }
                
//                if (bfProp.equals(BfProperty.BF_INSTANCE_TITLE)) {
//                    convertTitle(bfProp);
//                }
                                       
            }          
        }
        return super.convert();
    }

    private boolean moveTypeToRelatedResource(Resource type, 
            Resource relatedResource, Map<Resource, Resource> typeMap) {
        
        if (relatedResource != null) {
            if (typeMap.containsKey(type)) {
                outputModel.add(
                        relatedResource, RDF.type, typeMap.get(type));
                return true;
            }
            
        }
        return false;        
        
    }
    
    private void convertProvider(Statement statement) {
        
        BfResourceConverter converter = 
                new BfProviderConverter(this.localNamespace);
     
        // Identify the provider resource and build its associated model (i.e.,
        // statements in which it is the subject or object).
        Resource resource = statement.getResource();
        Model providerModel = getResourceSubModel(resource);
        Resource provider = providerModel.createResource(resource.getURI());
                      
        // Add BfProviderConverter model to this converter's outputModel model,
        // so they get added to the BibframeConverter output model.
        Model convert = converter.convert(provider);
		outputModel.add(convert);
		convert.close();
    }

}
