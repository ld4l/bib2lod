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
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.OntNamespace;

public abstract class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);
    
    protected Resource subject;
    protected Model model;
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
        this.model = subject.getModel();

        convertModel();
        
        return model;
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
        
        this.subject = subject;
        this.model = subject.getModel();
        this.linkingStatement = linkingStatement;
        
        convertModel();

        return model;        
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
    protected void convertModel() {
        
        // If this method is called from a subclass method which neglected to
        // apply retractions to the model, apply them now, so that they are not
        // reprocessed here.
        applyRetractions();
                    
        // Map of Bibframe to LD4L types.
        Map<Resource, Resource> typeMap = 
                BfType.typeMap(getBfTypesToConvert());
        
        List<Resource> typesToRetract = 
                BfType.ontClasses(getBfTypesToRetract());
                 
        // Map of Bibframe to LD4L properties.
        Map<Property, Property> propertyMap = BfProperty.propertyMap(
                getBfPropertiesToConvert(), getBfPropertyMap());

        // List of Bibframe properties to retract.
        List<Property> propsToRetract = 
                BfProperty.propertyList(getBfPropertiesToRetract());

        // Iterate through the statements in the model.
        StmtIterator stmts = model.listStatements();
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
            if (LOGGER.isInfoEnabled()) {
                if (! subject.equals(this.subject)) {
                    LOGGER.debug("Found statement " + stmt.toString() 
                            + " where subject is not " + this.subject.getURI());
                }
            }
                      
            if (predicate.equals(RDF.type)) {

                Resource type = object.asResource();
                
                // If new type has been specified, use it
                if (typeMap.containsKey(type)) {
                    outputModel.add(subject, predicate, typeMap.get(type));
                    stmts.remove();
                    
                } else if (typesToRetract.contains(type)) {
                    stmts.remove();
                
                // Change any remaining types in Bibframe namespace to LD4L
                // namespace.
                } else if (convertBfTypeNamespace(subject, type)) {
                    stmts.remove();
                    
                } // else: external namespace (e.g., madsrdf); don't modify

            } else if (propertyMap.containsKey(predicate)) {
                
                if (LOGGER.isInfoEnabled()) {
                    if (BfProperty.get(predicate).namespace().equals
                            (OntNamespace.LEGACY)) {
                        // Log for review, to determine what legacy properties 
                        // are being used, to inform future development.
                        LOGGER.debug("Adding statement with property in legacy "
                                + "namespace: " + predicate.getURI());
                    }
                }
                outputModel.add(subject, propertyMap.get(predicate), object);
                stmts.remove();
                
            } else if (propsToRetract.contains(predicate)) {
                stmts.remove(); 
              
            // Change any remaining predicates in Bibframe namespace to LD4L
            // namespace.
            } else if (convertBfPropertyNamespace(subject, predicate, object)) {
                stmts.remove();    
                
            } // else: external namespace (e.g., madsrdf); don't modify
        }
        
        model.add(outputModel);
 
    }   
    
    /*
     * Fall-through case: change namespace from Bibframe to LD4L. Don't modify
     * statements in external namespace (e.g., madsrdf).
     * 
     * Possibly we should reverse the default: change namespace if in a 
     * list of types, else discard. 
     */
    protected boolean convertBfTypeNamespace(Resource subject, Resource type) {
        
        String bfNamespace = OntNamespace.BIBFRAME.uri();
        String ld4lNamespace = OntNamespace.LD4L.uri();

        if (type.getNameSpace().equals(bfNamespace)) {
            
            Resource newType = model.createResource(
                    ld4lNamespace + type.getLocalName());
            
            // Log for dev purposes, to make sure we shouldn't have handled 
            // this type in a more specific way.
            LOGGER.debug("Changing resource " + type.getURI()
                    + " in Bibframe namespace to " 
                    + newType.getURI() + " in LD4L namespace.");
             
            outputModel.add(subject, RDF.type, newType);
            
            return true;
        }
        
        return false;
    }

    /*
     * Fall-through case: change namespace from Bibframe to LD4L. Don't modify
     * statements in external namespace (e.g., madsrdf).
     * 
     * Possibly we should reverse the default: change namespace if in a 
     * list of properties, else discard. 
     */
    protected boolean convertBfPropertyNamespace(Resource subject,
            Property predicate, RDFNode object) {
        
        String bfNamespace = OntNamespace.BIBFRAME.uri();
        String ld4lNamespace = OntNamespace.LD4L.uri();
        
        if (predicate.getNameSpace().equals(bfNamespace)) {  
            
            Property ld4lProp = model.createProperty(
                    ld4lNamespace, predicate.getLocalName());
            
            // Log for dev purposes, to make sure we shouldn't have handled 
            // this type in a more specific way.
            LOGGER.debug("Changing property " + predicate.getURI()
                    + " in Bibframe namespace to " + ld4lProp.getURI()
                    + " in LD4L namespace.");
            outputModel.add(subject, ld4lProp, object);
            return true;
        }
        
        return false;
    }

    protected List<BfType> getBfTypesToConvert() {
        List<BfType> typesToConvert = new ArrayList<BfType>();
        typesToConvert.add(this.bfType);
        return typesToConvert;
    }
    
    protected List<BfType> getBfTypesToRetract() {
        return new ArrayList<BfType>();
    }
    
    protected List<BfProperty> getBfPropertiesToConvert() {
        return new ArrayList<BfProperty>();
    }
    
    protected Map<BfProperty, Ld4lProperty> getBfPropertyMap() {
        return new HashMap<BfProperty, Ld4lProperty>();
    }
     
    protected List<BfProperty> getBfPropertiesToRetract() {
        return new ArrayList<BfProperty>();
    }

    /**
     * If there is a statement in the model with a property in the list, remove
     * the resource that is the object of the property from the model.
     * @param props
     */
    protected void removeResources(List<BfProperty> props) {
        for (BfProperty prop : props) {
            Resource resource = 
                    subject.getPropertyResourceValue(prop.property());
            if (resource != null) {
                removeResource(model, resource);
            }
        }
    }
    /**
     * Convenience methods to remove a resource from a Jena model. In Jena, this
     * is accomplished by removing all statements in which the resource is the
     * subject or the object.
     */
    protected void removeResource(Model model, Resource resource) {
        model.removeAll(resource, null, null);
        model.removeAll(null, null, resource);
    }
    
    protected void removeResource(Resource resource) {
        removeResource(model, resource);
    }
    
    protected void applyRetractions() {
        applyRetractions(model, retractions);
    }
    
    /*
     * Empty retractions model after adding to model, so if another conversion
     * process adds new retractions, the original statements don't get 
     * re-retracted.
     */
    protected void applyRetractions(Model model, Model retractions) {
        model.remove(retractions);
        // Empty retractions model so if another conversion process applies,
        // we won't re-retract the statements.
        retractions.removeAll();  
    }

}