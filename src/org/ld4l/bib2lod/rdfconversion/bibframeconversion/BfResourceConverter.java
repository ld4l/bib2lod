package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);
    
    protected OntType type;
    private String uriPostfix;

    
    public BfResourceConverter(OntType type) {
        LOGGER.trace("In constructor for " + this.getClass().getName());
        this.type = type;
        uriPostfix = type.namespace().prefix() + type.localname();
    }
    
    public Model convert(Resource subject) {  
        return subject.getModel();
    }
    
 
    
    /* -------------------------------------------------------------------------
     * 
     * Bridge utilities between Bib2Lod objects and Jena objects and models
     * 
     * -----------------------------------------------------------------------*/
    

    protected String mintUri(Resource resource) {
        return mintUri(resource.getURI());
    }
    
    protected String mintUri(String uri) {
        return uri + uriPostfix;
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
 
    protected Resource createResource(OntType type, Model model) {
        return model.createResource(type.uri());
    }
    
    protected void addStatement(String subjectUri, OntProperty ontProp, 
            Resource object, Model model) {
        
        Resource subject = model.createResource(subjectUri);
        addStatement(subject, ontProp, object, model);
    }
     
    protected void addStatement(Resource subject, OntProperty ontProp, 
            Resource object, Model model) {
        Property property = model.createProperty(ontProp.namespaceUri(), 
                ontProp.localname());
        model.add(subject, property, object);
    }
    
    protected String getBfLabel(Resource subject) {
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
    

}
