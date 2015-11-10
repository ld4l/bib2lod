package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

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
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.OntNamespace;

public abstract class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);
    
    protected Resource subject;
    protected Model model;
    

    // Public interface method: initialize instance variables and convert.
    public final Model convert(Resource subject) {    
        
        this.subject = subject;
        this.model = subject.getModel();
        convert();
        return model;
    }

    /* 
     * Default conversion method. Subclasses may override. 
     */
    protected void convert() {
        
        assignType();   
        convertProperties();
        retractProperties();  
        changePropertyNamespaces();       
    }


    /**
     * Remove existing type assertions and assign new type.
     */
    protected void assignType() {
        
        Ld4lType newType = getNewType();
        if (newType != null) {
            subject.removeAll(RDF.type);                   
            Resource ontClass = newType.ontClass();
            subject.addProperty(RDF.type,  ontClass);
        }
    }

    protected abstract Ld4lType getNewType();
    
    
    protected void convertProperties() {
        
        Map<BfProperty, Ld4lProperty> propertyMap = getPropertyMap();

        // TODO We may want to add a generic map of properties to convert for
        // all types. Then individual classes don't need to list these; e.g.
        // BF_HAS_AUTHORITY => MADSRDF_IDENTIFIED_BY_AUTHORITY.
        // propertyMap.addAll(UNIVERSAL_PROPERTY_MAP);
        // Note: subclasses should return an empty map rather than null, so
        // it can be added to.
        
        if (propertyMap != null) {        
            for (Map.Entry<BfProperty, Ld4lProperty> entry 
                    : propertyMap.entrySet()) {      
                convertProperty(entry.getKey(), entry.getValue());  
            }
        }
    }
    
    protected abstract Map<BfProperty, Ld4lProperty> getPropertyMap();

    /** 
     * Add a new statement based on a Bibframe statement, using the object of
     * the Bibframe statement as the object of the new statement. Remove the
     * original statement.
     * 
     * @param subject
     * @param oldProp
     * @param newProp
     * @return
     */
    protected void convertProperty(BfProperty oldProp, 
            Ld4lProperty newProp) {
                    
        Property oldProperty = oldProp.property();
        Statement stmt = subject.getProperty(oldProperty);
                
        if (stmt != null) {
            RDFNode object = stmt.getObject();
            Property newProperty = newProp.property();
            subject.addProperty(newProperty, object);
            subject.removeAll(oldProperty);
        }
    }
    
    /**
     * Retract statements with the specified predicates.
     */
    protected void retractProperties() {
        
        List<BfProperty> propertiesToRetract = getPropertiesToRetract();
        
        // TODO We may want to add a generic set of properties to retract from
        // all types. Then individual classes don't need to list these; e.g.
        // BF_AUTHORIZED_ACCESS_POINT.
        // propertiesToRetract.addAll(UNIVERSAL_PROPERTIES_TO_RETRACT);
        // Note: subclasses should return an empty list rather than null, so
        // it can be added to.
        
        if (propertiesToRetract != null) {
            for (BfProperty prop : propertiesToRetract) {
                LOGGER.debug("Removing property " + prop.uri());
                subject.removeAll(prop.property());
            }
        }
    }

    protected abstract List<BfProperty> getPropertiesToRetract();
    
    /**
     * After all specific conversions, change namespace of all remaining
     * properties from Bibframe to LD4L. 
     * 
     * TODO Do we need to do this with classes too??
     * 
     * TODO Does this need to be called from subclass convert() methods?
     * Check the output to see if there are any bibframe terms remaining.
     */
    protected void changePropertyNamespaces() {
        
        String bfNamespace = OntNamespace.BIBFRAME.uri();
        String ld4lNamespace = OntNamespace.LD4L.uri();
        
        Model assertions = ModelFactory.createDefaultModel();
        Model retractions = ModelFactory.createDefaultModel();
        
        StmtIterator stmts = model.listStatements();
        while (stmts.hasNext()) {
            Statement stmt = stmts.nextStatement();
            Property prop = stmt.getPredicate();
            String namespace = prop.getNameSpace();
            if (namespace.equals(bfNamespace)) {
                Property newProp = model.createProperty(
                        ld4lNamespace, prop.getLocalName());
                assertions.add(stmt.getSubject(), newProp, stmt.getObject());
                retractions.add(stmt);
            }
        }
        
        model.add(assertions);
        model.remove(retractions);
    }
}
