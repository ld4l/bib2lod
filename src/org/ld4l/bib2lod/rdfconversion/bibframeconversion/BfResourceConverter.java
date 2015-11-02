package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public abstract class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);
    
    protected Resource subject;
    // private String uriPostfix;

    
    public BfResourceConverter(Resource subject) {
        LOGGER.debug("In constructor for " + this.getClass().getName());
        
        // NB Currently a new converter is created for each subject resource,
        // so the subject can be assigned to an instance variable. If we 
        // change the flow so that a converter is created for an entire model of
        // subjects of a certain type, the subject will have to be passed to the
        // convert method rather than the constructor.
        this.subject = subject;

        // Possibly a converter could be shared by multiple types - e.g., 
        // BfFamilyConverter and BfOrganizationConverter could both be
        // BfAuthorityConverter. Then the original rdf:type must be passed
        // to the constructor so that we know what new type should be 
        // assigned. 
        // this.type = type;
        
        // uriPostfix = type.namespace().prefix() + type.localname();
    }

    /* 
     * Default conversion method. Subclasses may override. 
     */
    public Model convert() {       
        assignType();        
        convertProperties();
        retractProperties();    
        return subject.getModel();
    }

    
    /* -------------------------------------------------------------------------
     * 
     * Utilities bridging Bib2Lod objects and Jena objects and models
     * 
     * -----------------------------------------------------------------------*/
    
    /**
     * Remove existing type assertions and assign new type.
     */
    protected void assignType() {
        
        Ld4lType newType = getNewType();
        if (newType != null) {
            subject.removeAll(RDF.type);                   
            Resource ontClass = newType.resource();
            subject.addProperty(RDF.type,  ontClass);
        }
    }

    protected abstract Ld4lType getNewType();
    
    
    protected void convertProperties() {
        
        Map<BfProperty, Ld4lProperty> propertyMap = getPropertyMap();

        // We may want to add a generic map of properties to convert for
        // all types. Then individual classes don't need to list these; e.g.
        // BF_HAS_AUTHORITY => MADSRDF_IDENTIFIED_BY_AUTHORITY.
        // propertyMap.addAll(UNIVERSAL_PROPERTY_MAP);
        
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
    protected void convertProperty(BfProperty oldProp, Ld4lProperty newProp) {
                    
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
        
        // We may want to add a generic set of properties to retract from
        // all types. Then individual classes don't need to list these; e.g.
        // BF_AUTHORIZED_ACCESS_POINT.
        // propertiesToRetract.addAll(UNIVERSAL_PROPERTIES_TO_RETRACT);
        
        if (propertiesToRetract != null) {
            Model model = subject.getModel();
            for (BfProperty prop : propertiesToRetract) {
                LOGGER.debug("Removing property " + prop.uri());
                subject.removeAll(prop.property());
            }
        }
    }

    protected abstract List<BfProperty> getPropertiesToRetract();

    /**
     * Create a Jena resource from an Ld4lType
     * @param newProp - the Ld4lType
     * @param model - the model to create the property in
     * @return a Jena property
     */   
    protected Resource createResource(Ld4lType type, Model model) {
        return model.createResource(type.uri());
    }


//    protected void addStatement(String subjectUri, BfProperty ontProp, 
//            Resource object, Model model) {
//        
//        Resource subject = model.createResource(subjectUri);
//        addStatement(subject, ontProp, object, model);
//    }
     
//    protected void addStatement(Resource subject, BfProperty ontProp, 
//            Resource object, Model model) {
//        Property property = model.createProperty(ontProp.namespaceUri(), 
//                ontProp.localname());
//        model.add(subject, property, object);
//    }

    
//    protected Literal getLiteral(BfProperty ontProp) {
//        Property property = subject.getModel().getProperty(ontProp.uri());
//        Statement stmt = subject.getProperty(property);
//        if (stmt != null) {
//            return stmt.getLiteral();
//        }   
//        return null;
//    }
//    
//    protected String getLiteralValue(BfProperty ontProp) {
//        
//        Literal literal = getLiteral(ontProp);
//        if (literal != null) {
//            return literal.getLexicalForm();
//        }
//        return null;
//    }
    
//  protected String mintUri(Resource resource) {
//      return mintUri(resource.getURI());
//  }
//  
//  protected String mintUri(String uri) {
//      return uri + uriPostfix;
//  }
  


}
