package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.OntNamespace;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public abstract class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);
    
    protected Resource subject;
    protected Model model;
    protected Model assertions;
    // protected Model retractions;
    
    private static final List<Property> PROPERTIES_TO_RETRACT = 
            new ArrayList<Property>();
    static {
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_AUTHORITY_SOURCE.property());
        PROPERTIES_TO_RETRACT.add(
                BfProperty.BF_AUTHORIZED_ACCESS_POINT.property());
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
     */
    protected void convert() {
        
        // Map of Bibframe to LD4L types.
        Map<Resource, Resource> typeMap = BfType.typeMap();
        
        // Map of Bibframe to LD4L properties.
        // Start with the statically-defined, type-independent map.
        Map<Property, Property> propertyMap = BfProperty.propertyMap();
        // Override with any instance-level, type-specific mappings.
        Map<Property, Property> typeSpecificPropertyMap = 
                getPropertyMap();
        if (typeSpecificPropertyMap != null) {
            propertyMap.putAll(typeSpecificPropertyMap);
        }
 
        // List of Bibframe properties to retract.
        // Start with the statically-defined, type-independent list.
        List<Property> propsToRetract = PROPERTIES_TO_RETRACT;
        // Override with any instance-level, type-specific items.
        List<Property> typeSpecificPropsToRetract = getPropertiesToRetract();
        if (typeSpecificPropsToRetract != null) {
            propsToRetract.addAll(typeSpecificPropsToRetract);
        }
        
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
                if (typeMap.containsKey(ontClass)) {
                    assertions.add(subject, predicate, typeMap.get(ontClass));
                    stmts.remove();
                }      
              
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


    protected Map<Property, Property> getPropertyMap() {
        return null;
    }
    
    protected List<Property> getPropertiesToRetract() {
        return null;
    }
    
}