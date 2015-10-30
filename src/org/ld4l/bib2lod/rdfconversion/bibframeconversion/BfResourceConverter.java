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
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;

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
        
        OntType newType = getNewType();
        if (newType != null) {
            subject.removeAll(RDF.type);                   
            Resource ontClass = createResource(newType, subject.getModel());
            subject.addProperty(RDF.type,  ontClass);
        }
    }

    protected abstract OntType getNewType();
    
    
    protected void convertProperties() {
        
        Map<OntProperty, OntProperty> propertyMap = getPropertyMap();

        // We may want to add a generic map of properties to convert for
        // all types. Then individual classes don't need to list these; e.g.
        // BF_HAS_AUTHORITY => MADSRDF_IDENTIFIED_BY_AUTHORITY.
        // propertyMap.addAll(UNIVERSAL_PROPERTY_MAP);
        
        if (propertyMap != null) {        
            for (Map.Entry<OntProperty, OntProperty> entry 
                    : propertyMap.entrySet()) {      
                convertProperty(entry.getKey(), entry.getValue());  
            }
        }
    }
    
    protected abstract Map<OntProperty, OntProperty> getPropertyMap();

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
    protected void convertProperty(OntProperty oldProp, OntProperty newProp) {
                    
        Model model = subject.getModel();
        Property oldProperty = createProperty(oldProp, model);
        Statement stmt = subject.getProperty(oldProperty);
                
        if (stmt != null) {
            RDFNode object = stmt.getObject();
            Property newProperty = createProperty(newProp, model);
            subject.addProperty(newProperty, object);
            subject.removeAll(oldProperty);
        }
    }
    
    /**
     * Retract statements with the specified predicates.
     */
    protected void retractProperties() {
        
        List<OntProperty> propertiesToRetract = getPropertiesToRetract();
        
        // We may want to add a generic set of properties to retract from
        // all types. Then individual classes don't need to list these; e.g.
        // BF_AUTHORIZED_ACCESS_POINT.
        // propertiesToRetract.addAll(UNIVERSAL_PROPERTIES_TO_RETRACT);
        
        if (propertiesToRetract != null) {
            Model model = subject.getModel();
            for (OntProperty prop : propertiesToRetract) {
                LOGGER.debug("Removing property " + prop.uri());
                subject.removeAll(createProperty(prop, model));
            }
        }
    }

    protected abstract List<OntProperty> getPropertiesToRetract();

    
    protected Resource createResource(OntType type, Model model) {
        return model.createResource(type.uri());
    }
    
    /**
     * Create a Jena property from an OntProperty
     * @param property - the OntProperty
     * @param model - the model to create the property in
     * @return a Jena property
     */
    protected Property createProperty(OntProperty property, Model model) {
        return model.createProperty(property.namespaceUri(), 
                property.localname());
    }
 
//    protected Property createProperty(OntProperty property, Resource subject) {
//        return createProperty(property, subject.getModel());
//    }
    

    
//    protected void addStatement(String subjectUri, OntProperty ontProp, 
//            Resource object, Model model) {
//        
//        Resource subject = model.createResource(subjectUri);
//        addStatement(subject, ontProp, object, model);
//    }
     
//    protected void addStatement(Resource subject, OntProperty ontProp, 
//            Resource object, Model model) {
//        Property property = model.createProperty(ontProp.namespaceUri(), 
//                ontProp.localname());
//        model.add(subject, property, object);
//    }

    
//    protected Literal getLiteral(OntProperty ontProp) {
//        Property property = subject.getModel().getProperty(ontProp.uri());
//        Statement stmt = subject.getProperty(property);
//        if (stmt != null) {
//            return stmt.getLiteral();
//        }   
//        return null;
//    }
//    
//    protected String getLiteralValue(OntProperty ontProp) {
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
