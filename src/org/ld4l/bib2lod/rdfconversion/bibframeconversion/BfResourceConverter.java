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
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;

public class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);
  
    protected Resource subject;
    protected Model inputModel;
    protected BfType bfType;
    protected Model outputModel;
    protected Model retractions;
    protected String localNamespace;
    protected Statement linkingStatement;

    public BfResourceConverter(BfType bfType, String localNamespace) {
        this(localNamespace);     
        LOGGER.debug("In constructor for converter type " 
                + this.getClass().getSimpleName()
                + "; converting Bibframe type " + bfType);  
     
        this.bfType = bfType;
    }
    
    protected BfResourceConverter(String localNamespace) {
        LOGGER.debug("In constructor for converter type " 
                + this.getClass().getSimpleName() 
                + "; no Bibframe type specified.");

        this.localNamespace = localNamespace;
        
        this.outputModel = ModelFactory.createDefaultModel();
        this.retractions = ModelFactory.createDefaultModel();
        
    }

    /* 
     * This constructor is used when a converter is called from another 
     * converter. The statement parameter links the subject of the calling
     * converter (e.g., BfInstanceConverter) to the subject of this converter
     * (e.g., BfTitleConverter).
     * So far the known use cases don't require a BfType parameter, since these
     * converters handle only a single type. Can add the parameter, or another 
     * constructor, if needed later.  
     */
    protected BfResourceConverter(String localNamespace, Statement statement) {
        this(localNamespace);
        this.linkingStatement = statement;
    }

    /*
     * Public interface method. Defined so convertSubject() and convertModel() 
     * can be protected, so that if subclasses override these methods they
     * cannot be called from outside the package.
     */
    public final Model convert(Resource subject) {        
        return convertSubject(subject);
    }

    /*
     * Top level method: initialize instance variables and convert.
     * Subclasses may override: needed in the case where one converter calls
     * another. The called converter must pass back a converted model, with
     * outputModel and retractions applied, to the caller. The subject of the
     * called converter is also different from the caller's subject. Examples:
     * Instance converter calls Provider, Title, and Identifier converters.
     * Work converter calls Title converter.
     */
    protected Model convertSubject(Resource subject) {    
        
        this.subject = subject;
        this.inputModel = subject.getModel();

        return convertModel();
    }

    protected Model convertSubject(
            /* The linking statement is passed in when the converter is called
             * by another converter: the linking statement is the statement 
             * that relates the caller's subject to this subject. For example,
             * the statement :instance bf:publication :provider links the 
             * Instance to the Provider, when BfInstanceConverter calls
             * BfProviderConverter.
             */
            Resource subject, Statement linkingStatement) {

        this.linkingStatement = linkingStatement;
        
        return convertSubject(subject);       
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
    
        // Remove resources designated for removal so that statements containing
        // these resources are not processed in the iteration through the 
        // model statements.
        removeResources(getResourcesToRemove());

        // Iterate through the statements in the input model.
        StmtIterator stmts = inputModel.listStatements();
        while (stmts.hasNext()) {
            
            Statement stmt = stmts.nextStatement();
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

                // Handle mads types in typeMap, so individual converters are 
                // able to remove them.
                // } else if (type.getNameSpace().equals(
                //         OntNamespace.MADSRDF.uri())) {
                //     outputModel.add(stmt);
                    
                }
  
            } else if (propertyMap.containsKey(predicate)) {
                
                outputModel.add(subject, propertyMap.get(predicate), object);

            // Handle mads types in typeMap, so individual converters are able 
            // to remove them.
            // } else if (predicate.getNameSpace().equals(
            //           OntNamespace.MADSRDF.uri())) {
            //       outputModel.add(stmt);
                
            }
 
            // We could add this if there are any child converters that do 
            // additional processing AFTER the superclass method, so that the
            // statements aren't reprocessed. At this point we don't have such
            // cases.
            // stmt.remove();
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
        
        // Get default mapping from Bibframe to LD4l types
        Map<Resource, Resource> typeMap = BfType.typeMap();
        
        // Type-specific mappings will override
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
        
        // Get default mapping from Bibframe to LD4L properties
        Map<Property, Property> propertyMap = BfProperty.propertyMap();
        
        // Type-specific mapping will override
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

}