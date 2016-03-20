package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lIndividual;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfInstanceConverter extends BfResourceConverter {

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
    
    private static final List<BfProperty> PROPERTIES_TO_REMOVE = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_REMOVE.add(BfProperty.BF_SYSTEM_NUMBER);
    }
    
    public BfInstanceConverter(String localNamespace) {
        super(localNamespace);
    }
    
    @Override 
    protected Model convert() {

        LOGGER.debug("Converting instance " + subject.getURI());

        Resource relatedWork = getRelatedWork();        
        Resource relatedItem = getRelatedItem();

        StmtIterator stmts = subject.getModel().listStatements();
 
        boolean bfTitlePropDone = false;
        
        while (stmts.hasNext()) {
            Statement statement = stmts.nextStatement();            
            Property predicate = statement.getPredicate();
            RDFNode object = statement.getObject();
    
            if (predicate.equals(RDF.type)) {
                
                convertType(object.asResource(), 
                        relatedWork, relatedItem);
                
            } else {
                bfTitlePropDone = 
                        convertProperty(predicate, object,  bfTitlePropDone);
            }
        }            
        return super.convert();
    }

    private Resource getRelatedWork() {

        Resource relatedWork = null;
        
        Statement instanceOf = 
                subject.getProperty(BfProperty.BF_INSTANCE_OF.property());
        // Probably can't be null
        if (instanceOf != null) {
           relatedWork = instanceOf.getResource(); 
        }
                
        return relatedWork;
    }
    
    private Resource getRelatedItem() {
        
        Resource relatedItem = null;

        // Get any associated Items
        List<Statement> hasHolding = subject.getModel().listStatements(
                null, BfProperty.BF_HOLDING_FOR.property(), subject).toList();
        
        // Assign relatedItem only if there's just one. Otherwise, we don't
        // know which Item the type should apply to.
        if (hasHolding.size() == 1) {
            relatedItem = hasHolding.get(0).getSubject();
        }

        return relatedItem;
    }
    
    private void convertType(Resource object, 
            Resource relatedWork, Resource relatedItem) {

        if (moveTypeToRelatedResource(
                object.asResource(), relatedWork, WORK_TYPE_MAP)) {
        
        } else if (moveTypeToRelatedResource(
                object.asResource(), relatedItem, ITEM_TYPE_MAP)) {
            
        } // else default type conversions handled in super.convert()
        
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

    private boolean convertProperty(
            Property predicate, RDFNode object, boolean bfTitlePropDone) {
        
        BfProperty bfProp = BfProperty.get(predicate);
        
        if (bfProp == null) {
            // Log for review, to make sure nothing has escaped.
            LOGGER.debug("No handling defined for property " 
                    + predicate.getURI()
                    + "; deleting statement.");
            return bfTitlePropDone;
        }

        if (bfProp.equals(BfProperty.BF_SYSTEM_NUMBER)) {

            convertSystemNumber(object);

        // Only Instances have bf:titleStatement assertions
        } else if (bfProp.equals(BfProperty.BF_TITLE_STATEMENT)) {
            convertBfTitleStatement(object.asLiteral());            

        } else if (bfProp.equals(BfProperty.BF_TITLE)) {
            
            // If there are two bf:title statements, they are converted
            // together in BfTitleConverter.convertBfTitleProp(), so
            // don't reprocess the second one.
            if (!bfTitlePropDone) {
                convertBfTitle(object.asLiteral());
                bfTitlePropDone = true;
            }   
        }  
        
        return bfTitlePropDone;
    }         
    
    private void convertSystemNumber(RDFNode object) {
 
        // Harvard contains some cases where bf:systemNumber has a
        // literal object. These have been handled earlier in 
        // convertIdentifierLiteral().
        // TODO **** Make sure this is true.
        if (object.isResource()) {
            
            Resource identifier = object.asResource();

            // Add owl:sameAs to a WorldCat URI
            // WorldCat local names start with a digit, so can't use Jena
            // Resource.getNameSpace().
            if (identifier.getURI().startsWith(Vocabulary.WORLDCAT.uri())) {
                LOGGER.debug("Adding " + subject.getURI() 
                        + " owl:sameAs " + identifier.getURI());
                outputModel.add(subject, OWL.sameAs, identifier);
                createWorldCatIdentifier(identifier);   
                
            } else {
                outputModel.add(subject,  
                        Ld4lProperty.IDENTIFIED_BY.property(), 
                        identifier);
            }
        }        
    }
    
    /*
     * bf:titleStatement is used only with an Instance, and indicates a 
     * transcribed title. This is expressed in LD4L with the 
     * ld4l:hasSourceStatus predicate.
     */
    private void convertBfTitleStatement(Literal value) {
  
        Resource title = createTranscribedTitle(value);
        Model titleModel = title.getModel();
        outputModel.add(titleModel);
        //outputModel.add(subject, Ld4lProperty.HAS_TITLE.property(), title);
    }
    
    // Convert bf:title statement
    private void convertBfTitle(Literal value) {

        Model titleModel = 
                TitleUtils.convertBfTitleDataProp(subject, localNamespace);  
                               
        if (titleModel != null) {
            outputModel.add(titleModel);
            titleModel.close();
        }               
    }
 
    /*
     * Only an Instance may have a TranscribedTitle.
     */
    private Resource createTranscribedTitle(Literal titleLiteral) {
                    
        Resource title = TitleUtils.createSimpleTitle(
                subject, titleLiteral, null, localNamespace);
        
        title.addProperty(Ld4lProperty.HAS_SOURCE_STATUS.property(), 
                Ld4lIndividual.SOURCE_STATUS_TRANSCRIBED.individual());
        
        return title;      
    }
    
    private void createWorldCatIdentifier(Resource worldCat) {
        
        // Add an identifier object with the WorldCat id as its value.
        // This is wanted in addition to the owl:sameAs assertion from the 
        // instance to the WorldCat URI, in order to have easy access to the
        // identifier string value.
        Resource identifier = outputModel.createResource(
                RdfProcessor.mintUri(localNamespace));
        outputModel.add(subject, Ld4lProperty.IDENTIFIED_BY.property(), 
                identifier);
        outputModel.add(identifier, RDF.type, 
                Ld4lType.OCLC_IDENTIFIER.type());
        
        // Jena doesn't recognize localname starting with digit, so we need to
        // parse it ourselves.
        // String id = worldCat.getLocalName();
        String id = worldCat.getURI().replaceAll(Vocabulary.WORLDCAT.uri(), "");
        outputModel.add(identifier, RDF.value, id);
        
    }
    
    protected Map<Property, Property> getPropertyMap() {
        
        // WRONG - alters map returned by BfProperty.propertyMap()
        // Map<Property, Property> propertyMap = BfProperty.propertyMap();      
        // propertyMap.putAll(getPropertyMap());
        Map<Property, Property> propertyMap = new HashMap<Property, Property>();
        
        // Get default mapping from Bibframe to LD4L properties
        propertyMap.putAll(BfProperty.propertyMap());
        
        // The :instance bf:systemNumber :id statement should be removed when
        // :id is a WorldCat id, but retained otherwise. The easiest way to do
        // this is to take care of the property here, and not let it go through
        // default handling in super.convert(), where the object is not
        // inspected.
        propertyMap.remove(BfProperty.BF_SYSTEM_NUMBER.property());
        
        return propertyMap;
    }
    
}
