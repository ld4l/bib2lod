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
    protected Model assertions;
    // protected Model retractions;

    // TODO Make abstract if not instantiated ***
    public BfResourceConverter(BfType bfType) {
        LOGGER.debug("In constructor for converter type " 
                + this.getClass().getSimpleName()
                + "; converting Bibframe type " + bfType);  
                 
        this.bfType = bfType;
    }

    // Public interface method: initialize instance variables and convert.
    public final Model convert(Resource subject) {    
        
        this.subject = subject;
        this.model = subject.getModel();
        
        assertions = ModelFactory.createDefaultModel();
        // retractions = ModelFactory.createDefaultModel();
        
        convert();
        
        model.add(assertions);
        // model.remove(retractions);
        
        return model;
    }

    /* 
     * Default conversion method. Subclasses may override.
     * 
     * The general strategy is to add new statements to the assertions model,
     * which then get added to the model after processing all conversions. In
     * some subclasses, new statements need to be added to the model immediately
     * so that they can be reprocessed. For example, in BfLanguageConverter, 
     * new statements must be reprocessed to replace local language URIs with
     * external ones.  
     * Retractions are removed immediately, so we don't need a retractions 
     * model. (We could change this in order to make the strategy uniform, but
     * the results will not be affected.)
     */
    protected void convert() {
        
        // Map of Bibframe to LD4L types.
        Map<Resource, Resource> typeMap = 
                BfType.typeMap(getBfTypesToConvert());
                
        // Map of Bibframe to LD4L properties.
        Map<Property, Property> propertyMap = BfProperty.propertyMap(
                getBfPropertiesToConvert(), getBfPropertyMap());

        // List of Bibframe properties to retract.
        List<Property> propsToRetract = 
                BfProperty.propertyList(getBfPropertiesToRetract());

        String bfNamespace = OntNamespace.BIBFRAME.uri();
        String ld4lNamespace = OntNamespace.LD4L.uri();
        
        // Iterate through the statements in the model.
        StmtIterator stmts = model.listStatements();
        while (stmts.hasNext()) {
            
            Statement stmt = stmts.nextStatement();
            Resource subject = stmt.getSubject();
            Property predicate = stmt.getPredicate();
            RDFNode object = stmt.getObject();
                       
            if (predicate.equals(RDF.type)) {
                Resource ontClass = object.asResource();
                Resource newOntClass;
                // If new type has been specified, use it
                if (typeMap.containsKey(ontClass)) {
                    newOntClass = typeMap.get(ontClass);
                // Otherwise change type namespace from Bibframe to LD4L
                } else if (ontClass.getNameSpace().equals(bfNamespace)) {
                    newOntClass = model.createResource(
                            ld4lNamespace + ontClass.getLocalName());  
                // Keep non-mapped types in non-Bibframe namespace
                } else {
                    continue;
                }
                assertions.add(subject, predicate, newOntClass);
                stmts.remove();

            } else if (propertyMap.keySet().contains(predicate)) {
                assertions.add(subject, propertyMap.get(predicate), object);
                stmts.remove();
                
            } else if (propsToRetract.contains(predicate)) {
                stmts.remove(); 
              
            // Change any remaining predicates in Bibframe namespace to LD4L
            // namespace.
            } else if (predicate.getNameSpace().equals(bfNamespace)) {             
                Property ld4lProp = model.createProperty(
                        ld4lNamespace, predicate.getLocalName());
                assertions.add(subject, ld4lProp, object);
                stmts.remove();               
            } 
        } 
    }   

    protected List<BfType> getBfTypesToConvert() {
        List<BfType> typesToConvert = new ArrayList<BfType>();
        typesToConvert.add(this.bfType);
        return typesToConvert;
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
     * Convenience method to remove a resource from a Jena model. In Jena, this
     * is accomplished by removing all statements in which the resource is the
     * subject or the object.
     */
    protected void removeResource(Model model, Resource resource) {
        model.removeAll(resource, null, null);
        model.removeAll(null, null, resource);
    }
    

}