package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.List;

import org.apache.jena.rdf.model.Literal;
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
    
    protected OntType type;
    protected Resource subject;
    // private String uriPostfix;

    
    public BfResourceConverter(OntType type, Resource subject) {
        LOGGER.debug("In constructor for " + this.getClass().getName());
        this.type = type;
        
        // NB Currently a new converter is created for each subject resource,
        // so the subject can be assigned to an instance variable. If we 
        // change the flow so that a converter is created for an entire model of
        // subjects of a certain type, the subject will have to be passed to the
        // convert function.
        this.subject = subject;
        
        // uriPostfix = type.namespace().prefix() + type.localname();
    }
    
    public Model convert() {  
        return subject.getModel();
    }

    
    /* -------------------------------------------------------------------------
     * 
     * Utilities bridging Bib2Lod objects and Jena objects and models
     * 
     * -----------------------------------------------------------------------*/
    
    protected abstract List<OntProperty> getPropertiesToRetract();
    
    /** 
     * Add a new statement based on a Bibframe statement, using the object of
     * the Bibframe statement as the object of the new statement. Currently the 
     * original statement is removed with other statements by calling 
     * retractProperties(), rather than removing it here.
     * 
     * @param subject
     * @param oldProp
     * @param newProp
     * @return
     */
    protected void addProperty(OntProperty oldProp, OntProperty newProp) {
            
        
        Model model = subject.getModel();

        Statement stmt = subject.getProperty(createProperty(oldProp, model));
                
        if (stmt != null) {
            // Resource object = stmt.getResource();
            RDFNode object = stmt.getObject();
            subject.addProperty(createProperty(newProp, model), object);
        }
    }
    
    protected void retractProperties() {
    
        Model model = subject.getModel();
        for (OntProperty prop : getPropertiesToRetract()) {
            LOGGER.debug("Removing property " + prop.uri());
            subject.removeAll(createProperty(prop, model));
        }
    }
    

//    protected String mintUri(Resource resource) {
//        return mintUri(resource.getURI());
//    }
//    
//    protected String mintUri(String uri) {
//        return uri + uriPostfix;
//    }
    
    
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
    
    protected Resource createResource(OntType type, Model model) {
        return model.createResource(type.uri());
    }
    
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
    
    protected String getBfLabelValue(Resource subject) {
        return getLiteralValue(subject, OntProperty.BF_LABEL);
    }
    
    protected String getLiteralValue(Resource subject, OntProperty ontProp) {
        String value = null;
        Property property = subject.getModel().getProperty(ontProp.uri());
        Statement stmt = subject.getProperty(property);
        if (stmt != null) {
            Literal literal = stmt.getLiteral();
            if (literal != null) {
                value = literal.getLexicalForm();
            }
        }
        return value;
    }
    
    protected void addType(OntType type) {
        Model model = subject.getModel();
        subject.addProperty(RDF.type, createResource(type, model));
    }
    

}
