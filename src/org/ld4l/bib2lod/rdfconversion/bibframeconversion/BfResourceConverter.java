package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;

public class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);

    /* 
     * The types of resources that should be included with the related primary
     * type resource (Work, Instance, Topic, Person, etc.) when building a model 
     * for that resource for ld4l conversion. 
     * Note that this is the complement of the CONVERTERS_BY_TYPE.keySet(). If
     * they have a specific converter, the converter is called from the 
     * converter of the related primary resource, not from here. It's also the
     * set of types that TypeSplitter adds to the file of the primary type.
     * TODO Couldl use this fact to streamline the queries in TypeSplitter and
     * building a subjectModel in getSubjectModelToConvert() below.
     */
    protected static final List<Resource> SECONDARY_TYPES = 
            new ArrayList<Resource>();
    static {
        SECONDARY_TYPES.add(BfType.BF_ANNOTATION.ontClass());
        SECONDARY_TYPES.add(BfType.BF_CATEGORY.ontClass());
        SECONDARY_TYPES.add(BfType.BF_CLASSIFICATION.ontClass());
        SECONDARY_TYPES.add(BfType.BF_IDENTIFIER.ontClass());
        SECONDARY_TYPES.add(BfType.BF_PROVIDER.ontClass());
        SECONDARY_TYPES.add(BfType.BF_TITLE.ontClass());
        SECONDARY_TYPES.add(BfType.MADSRDF_AUTHORITY.ontClass());   
    }
    
    protected Resource subject;
    protected Model inputModel;
    protected BfType bfType;
    protected Model outputModel;
    protected Model retractions;
    protected String localNamespace;
    protected Statement linkingStatement;

    public BfResourceConverter(BfType bfType, String localNamespace) {
        this(localNamespace);     
        this.bfType = bfType;
    }
    
    protected BfResourceConverter(String localNamespace) {
        this.localNamespace = localNamespace;
    }

    /* 
     * This constructor is used when a converter is called from another 
     * converter. The statement parameter links the subject of the calling
     * converter (e.g., BfInstanceConverter) to the subject of this converter
     * (e.g., BfTitleConverter).
     * So far the known use cases don't require a BfType parameter, since these
     * converters handle only a single type. Add the parameter, or another 
     * constructor, if needed later.  
     */
    protected BfResourceConverter(String localNamespace, Statement statement) {
        this(localNamespace);
        this.linkingStatement = statement;
    }

    /*
     * Public interface method. 
     */
    public final Model convert(Resource subject) {         
        init(subject);       
        return convertModel();
    }
    
    // Reset converter for processing of new subject
    private void init(Resource subject) {

        this.subject = subject;
        this.inputModel = subject.getModel();
        
        // Start with new, empty models for each conversion
        this.outputModel = ModelFactory.createDefaultModel();
        this.retractions = ModelFactory.createDefaultModel();  
    }
    
    /* 
     * Default conversion method. Subclasses may override.
     * 
     * The general strategy is to add new statements to the outputModel model,
     * which then get added to the model after processing all conversions. In
     * some subclasses, new statements need to be added to the model immediately
     * so that they can be reprocessed. For example, in BfLanguageConverter, 
     * new statements must be reprocessed to replace local language URIs with
     * external ones.  
     * Retractions are removed immediately, so we don't need a retractions 
     * model. (We could change this in order to make the strategy uniform, but
     * the results will not be affected.) A retractions model has been defined
     * anyway, to give the subclasses more flexibility.
     */
    protected Model convertModel() {
        
        // If this method is called from a subclass method which neglected to
        // apply retractions to the model, apply them now, so that they are not
        // reprocessed here.
        applyRetractions();

        // Map of Bibframe to LD4L types.
        Map<Resource, Resource> typeMap = buildTypeMap();
        
        // Map of Bibframe to LD4L properties.
        Map<Property, Property> propertyMap = buildPropertyMap();
        
        List<Property> identifierProps = 
                BfIdentifierConverter.getIdentifierProps();
    
        // Remove resources designated for removal so that statements containing
        // these resources are not processed in the iteration through the 
        // model statements.
        removeResources(getResourcesToRemove());

        List<Statement> stmts = inputModel.listStatements().toList();
            
        for (Statement stmt : stmts) {
            Property predicate = stmt.getPredicate();
            RDFNode object = stmt.getObject();
            
            // NB Normally stmt.getSubject() should be this.subject. However,
            // the subjectModel may contain statements with other subjects.
            // Normally these should have been converted by another converter
            // called from this converter, and then removed from this.model, 
            // but in case that hasn't been done, we can't assume the subject
            // is this.subject. This should be logged for further investigation.
            Resource subject = stmt.getSubject();
            if (LOGGER.isDebugEnabled()) {
                if (! subject.equals(this.subject)) {
                    LOGGER.debug("Found statement " + stmt.toString() 
                            + " where subject is not " + this.subject.getURI());
                }
            }
     
            if (predicate.equals(RDF.type)) {

                Resource type = object.asResource();                   
                if (typeMap.containsKey(type)) {
                    outputModel.add(subject, RDF.type, typeMap.get(type));
                }
  
           } else if (identifierProps.contains(predicate)) {               
                convertIdentifier(stmt);

            } else if (propertyMap.containsKey(predicate)) {               
                outputModel.add(subject, propertyMap.get(predicate), object);                
            }
 
            // We could add this if there are any child converters that do 
            // additional processing AFTER the superclass method, so that the
            // statements aren't reprocessed. Currently we don't have such
            // cases.
            // stmts.remove(stmt);
        }
        
        return outputModel;  
    }   
           
    /*
     * Resources to remove are expressed as a list of properties, because it is
     * easiest to identify the objects of those properties and remove all the
     * statements it occurs in either as subject or object. 
     */
    protected List<Property> getResourcesToRemove() {
        return new ArrayList<Property>();
    }
      
    private Map<Resource, Resource> buildTypeMap() {
        
        // WRONG - alters map returned by BfType.typeMap()
        // Map<Resource, Resource> typeMap = BfType.typeMap();
        // typeMap.putAll(getTypeMap());
        
        Map<Resource, Resource> typeMap = new HashMap<Resource, Resource>();
        // Get default mapping from Bibframe to LD4L properties
        typeMap.putAll(BfType.typeMap());
        // Type-specific mappings override
        typeMap.putAll(getTypeMap());
        
        // If a child converter needs to delete rather than convert a type,
        // remove it from the map. One case is where FAST topics remove 
        // the relationship to a madsrdf:Authority.
        List<Resource> typesToRetract = getTypesToRetract();
        typeMap.keySet().removeAll(typesToRetract);
        
        return typeMap;
    }
    
    protected Map<Resource, Resource> getTypeMap() {
        return new HashMap<Resource, Resource>();
    }

    protected List<Resource> getTypesToRetract() {
        return new ArrayList<Resource>();
    }
    
    private Map<Property, Property> buildPropertyMap() {
        
        // WRONG - alters map returned by BfProperty.propertyMap()
        // Map<Property, Property> propertyMap = BfProperty.propertyMap();      
        // propertyMap.putAll(getPropertyMap());

        Map<Property, Property> propertyMap = new HashMap<Property, Property>();
        // Get default mapping from Bibframe to LD4L properties
        propertyMap.putAll(BfProperty.propertyMap());
        // Type-specific mapping override
        propertyMap.putAll(getPropertyMap());

        List<Property> propertiesToRetract = getPropertiesToRetract();
        propertyMap.keySet().removeAll(propertiesToRetract);
        
        return propertyMap;
    }

    protected Map<Property, Property> getPropertyMap() {
        return new HashMap<Property, Property>();
    }

    protected List<Property> getPropertiesToRetract() {
        return new ArrayList<Property>();
    }
    
    /**
     * If there is a statement in the model with a property in the list, remove
     * the resource that is the object of the property from the model.
     * @param props
     */
    protected void removeResources(List<Property> props, Model model) {
        for (Property prop : props) {
            Resource resource = 
                    subject.getPropertyResourceValue(prop);
            if (resource != null) {
                removeResource(resource, model);
            }
        }
    }
    
    protected void removeResources(List<Property> props) {
        removeResources(props, inputModel);
    }
    
    /**
     * Convenience methods to remove a resource from a Jena model. In Jena, this
     * is accomplished by removing all statements in which the resource is the
     * subject or the object.
     */
    protected void removeResource(Resource resource, Model model) {
        model.removeAll(resource, null, null);
        model.removeAll(null, null, resource);
    }
    
    
    protected void applyRetractions() {
        applyRetractions(inputModel, retractions);
    }
    
    /*
     * Empty retractions model after adding to model, so if another conversion
     * process adds new retractions, the original statements don't get 
     * re-retracted.
     */
    protected void applyRetractions(Model model, Model retractions) {
        model.remove(retractions);
        retractions.removeAll();  
    }
    
    /*
     * Construct a subset of the input model that pertains to the specified 
     * resource. This includes statements where the resource is the subject
     * or the object, as well as some statements in which the related objects 
     * and subject are subjects.
     * Note that the available statements have already been constrained by 
     * TypeSplitter, since some statements that meet the criteria below have
     * already been written to different files.
     */
    public Resource getSubjectModelToConvert(Resource resource) {
        
        LOGGER.debug("Getting model for subject " + resource.getURI());
            
        // LOGGER.debug("Constructing subject model for " + resource.getURI());
        Model inputModel = resource.getModel();
        
        // Create the new model of statements related to this subject
        Model modelForSubject = ModelFactory.createDefaultModel();
        
        // Start with statements of which this resource is the subject
        List<Statement> resourceAsSubjectStmts = 
                resource.listProperties().toList();
        modelForSubject.add(resourceAsSubjectStmts);
    
        /* 
         * If the object of one of these statements is of a "secondary"  
         * type, add statements with the object as subject to the model. 
         * I.e., DON'T include statements about a work which, is, say the 
         * object of a :work bf:relatedWork :work statement - these should
         * be handled when that work is converted (i.e., on another 
         * iteration of the main loop over subjects in the input model. DO 
         * include statements about Identifiers, Titles, Annotations, etc. 
         * which are objects of statements about the current subject 
         * resource. Note that the statements of this sort that are 
         * included in the current inputModel have already been constrained 
         * in TypeSplitter.
         */ 
        for (Statement stmt : resourceAsSubjectStmts) {
            modelForSubject.add(getRelatedResourceStmts(stmt.getObject()));              
        }                
        
        // Then add statements with the current resource as object. Examples:
        // In BfLanguageConverter: :work bf:language :language
        // In BfWorkConverter: :annotation bf:annotates :work
        // However, note that in :work1 [related-work-predicate] :work2, the
        // statement gets added to the model for whichever work is processed
        // first. We must be prepared to handle the statement in either case -
        // i.e., from the subject or object point of view, OR override this
        // method in BfWorkConverter if we must ensure that it goes with the 
        // subject work or the object work.
        List<Statement> resourceAsObjectStmts = 
                inputModel.listStatements(null, null, resource).toList();
        
        if (! resourceAsObjectStmts.isEmpty()) {
            modelForSubject.add(resourceAsObjectStmts);

            /* 
             * If the subject of one of these statements is of a "secondary"  
             * type, add statements with the subject as subject to the model. 
             * I.e., DON'T include statements about a work which, is, say the 
             * object of a :work bf:relatedWork :work statement - these should
             * be handled when that work is converted (i.e., on another 
             * iteration of the main loop over subjects in the input model). DO 
             * include statements about Identifiers, Titles, Annotations, etc. 
             * which are objects of statements about the current subject 
             * resource. Note that the statements of this sort that are 
             * included in the current inputModel have already been constrained 
             * in TypeSplitter.
             */           
            for (Statement stmt : resourceAsObjectStmts) {
				Model related = getRelatedResourceStmts(stmt.getSubject());
				modelForSubject.add(related);
				related.close();
            }  
        }
        
        // Other converters will not need to process these statements, so they 
        // can be removed from the input model.
        inputModel.remove(modelForSubject);
        
        // RdfProcessor.printModel(modelForSubject, Level.DEBUG);
           
        // NB At this point, resource.getModel() is the inputModel, not 
        // the modelForSubject. Get the subject of the modelForSubject 
        // instead.            
        return modelForSubject.getResource(resource.getURI());               
    } 
    
    /* 
     * RDFNode node is a node related to the current subject, either by a 
     * statement where the subject is the subject and the node is the object, 
     * or the reverse. If this node is of a "secondary" type, such as an
     * Identifier, Annotation, Classification, Title, Provider, etc., add 
     * statements in which it is the subject to the model, since these 
     * statements should be converted with the primary subject.
     * 
     * TODO Consider: this complication may be eliminated if we were to do
     * a second type-splitting after resource deduping, putting secondary
     * types in their own files. The question is whether the secondary types
     * can be converted on their own, without information about their related
     * resource of primary type. This might not always be possible - e.g., 
     * see how the Instance bf:titleStatement is eliminated if another Title
     * with the same label exists. That requires having all the Instance's
     * title information together when converting the Instance.
     */
    protected Model getRelatedResourceStmts(RDFNode node) {
            
        Model model = ModelFactory.createDefaultModel();
        
        if (! node.isResource()) {
            return model;
        }
         
        Resource resource = node.asResource();

        // If this method is called from a converter that is passing off
        // processing of a secondary type, we don't need to collect further
        // statements about that primary resource. If this method is called
        // from BibframeConverter for a primary resource, this.subject is null.
        if (this.subject != null && resource.equals(this.subject)) {
            return model;
        }
        
        List<Statement> typeStmts = resource.listProperties(RDF.type).toList();
        for (Statement typeStmt : typeStmts) {
            Resource type = typeStmt.getResource();
            if (SECONDARY_TYPES.contains(type)) {
                model.add(resource.listProperties());     
                break;
            }
        }
        
        return model;
    }
   
    protected void convertIdentifier(Statement statement) {
        
        BfResourceConverter converter = new BfIdentifierConverter(
                this.localNamespace, statement);
     
        // Identify the identifier resource and build its associated model 
        // (i.e., statements in which it is the subject or object).
        Resource identifier = getSubjectModelToConvert(statement.getResource());
                      
        // Add BfIdentifierConverter model to this converter's outputModel 
        // model.
        Model convert = converter.convert(identifier);
		outputModel.add(convert);
		convert.close();
    }
}