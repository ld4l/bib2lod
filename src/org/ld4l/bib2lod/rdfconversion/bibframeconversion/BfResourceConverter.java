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
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_AUTHORITY_SOURCE);
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_AUTHORIZED_ACCESS_POINT);
    }


    // Public interface method: initialize instance variables and convert.
    public final Model convert(Resource subject) {    
        
        this.subject = subject;
        this.model = subject.getModel();
        
        convert();
        
        return model;
    }

    /* 
     * Default conversion method. Subclasses may override.
     * 
     * NB If subclasses don't override convert() but only the methods it calls,
     * combine this with the public convert() method.
     * 
     * TODO *** Consider whether it's better to do the conversions in one large
     * iteration through the statements of the model, rather than broken down
     * by property, class, etc.
     */
    protected void convert() {
        mapTypes();  
        convertProperties();  
    }

    /**
     * Convert Bibframe type assertion to corresponding LD4L type
     */
    protected void mapTypes() {
        
        /* 
         * NB We have to apply assertions and retractions to the input model at
         * each step of the conversion, rather than building up one large 
         * assertions and one large retractions model. Otherwise, the assertions
         * model will contain partially-converted statements that then get
         * added to the input model.
         */
        Model assertions = ModelFactory.createDefaultModel();
        // Model retractions = ModelFactory.createDefaultModel();

        // Start with the default, non-context-specific mappings for each type 
        // defined in BfType.
        Map<BfType, Ld4lType> typeMap = BfType.typeMap();
        
        // If there are any type-specific (i.e., static) or resource-specific
        // (i.e., instance) mappings, they override the default mappings.
        // Currently we have none.
//        Map<BfType, Ld4lType> typeSpecificMap = getTypeSpecificMap();
//        if (typeSpecificMap != null) {
//            typeMap.putAll(typeSpecificMap);
//        }

        for (Map.Entry<BfType, Ld4lType> entry : typeMap.entrySet()) {
            Resource bfOntClass = entry.getKey().ontClass();
            Resource ld4lOntClass = entry.getValue().ontClass();
            StmtIterator typeStmts = model.listStatements(
                    null, RDF.type, bfOntClass);
            // retractions.add(typeStmts);
            while (typeStmts.hasNext()) {
                Statement stmt = typeStmts.nextStatement();
                assertions.add(stmt.getSubject(), RDF.type, ld4lOntClass);
                // retractions.add(stmt);
                typeStmts.remove();
            }   
        }
        
        model.add(assertions);
             // .remove(retractions);
    }
    
    protected void convertProperties() {
        // Call mapProperties() before retractProperties() in case a subclass
        // overrides mapProperties(), but needs some of the information from
        // statements with predicates to be retracted.
        mapProperties();
        retractProperties();
        convertNamespace();   
    }
    
    protected void mapProperties() {

        Model assertions = ModelFactory.createDefaultModel();
        // Model retractions = ModelFactory.createDefaultModel();
      
        // Start with the default, non-context-specific mappings for each
        // property defined in BfProperty.
        Map<BfProperty, Ld4lProperty> propertyMap = BfProperty.propertyMap();
        // If there are any type-specific (i.e., static) or resource-specific
        // (i.e., instance) mappings, they override the default mappings. E.g., 
        // bf:label may be mapped to rdfs:label or foaf:name, depending on the
        // subject type.
        // Note: currently we define these only at the static level, for all 
        // instances of a type. 
        Map<BfProperty, Ld4lProperty> typeSpecificPropertyMap = 
                getPropertyMap();
        if (typeSpecificPropertyMap != null) {
            propertyMap.putAll(typeSpecificPropertyMap);
        }
        
        LOGGER.debug(propertyMap.toString());
      
        // Would it be more efficient to turn this around, and iterate over
        // the statements and check if the property exists in the map? 
        for (Map.Entry<BfProperty, Ld4lProperty> entry 
                : propertyMap.entrySet()) {
            Property bfProp = entry.getKey().property();
            Property ld4lProp = entry.getValue().property();
            StmtIterator stmts = model.listStatements(
                    null, bfProp, (RDFNode) null);
            // Oddly, this leaves stmts with no statements in it.
            //retractions.add(stmts);
            while (stmts.hasNext()) {
                Statement stmt = stmts.nextStatement();
                assertions.add(stmt.getSubject(), ld4lProp, stmt.getObject());
                //retractions.add(stmt);
                stmts.remove(); 
            }   
        } 
        

        model.add(assertions);
             // .remove(retractions);
    }

    protected void retractProperties() {

        List<BfProperty> propsToRetract = PROPERTIES_TO_RETRACT;
        List<BfProperty> typeSpecificPropsToRetract = getPropertiesToRetract();
        if (typeSpecificPropsToRetract != null) {
            propsToRetract.addAll(typeSpecificPropsToRetract);
        }
        
        for (BfProperty bfProp : propsToRetract) {
            StmtIterator stmts = model.listStatements(
                    null, bfProp.property(), (RDFNode) null);
            model.remove(stmts);
        }

    }
    
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
//    protected void convertProperty(BfProperty oldProp, 
//            Ld4lProperty newProp) {
//                    
//        Property oldProperty = oldProp.property();
//        Statement stmt = subject.getProperty(oldProperty);
//                
//        if (stmt != null) {
//            RDFNode object = stmt.getObject();
//            Property newProperty = newProp.property();
//            subject.addProperty(newProperty, object);
//            subject.removeAll(oldProperty);
//        }
//    }
    
    protected Map<BfProperty, Ld4lProperty> getPropertyMap() {
        return null;
    }
    
    protected List<BfProperty> getPropertiesToRetract() {
        return null;
    }
    
    /**
     * Cleanup operation: after all specific conversions, change namespace of 
     * all remaining Bibframe properties to LD4L. 
     * 
     * NB In this case, a renaming should be enough, since any semantics of the
     * predicate should be carried over.
     */
    protected void convertNamespace() {

        String bfNamespace = OntNamespace.BIBFRAME.uri();
        String ld4lNamespace = OntNamespace.LD4L.uri();

        Model assertions = ModelFactory.createDefaultModel();

        StmtIterator stmts = model.listStatements();
        while (stmts.hasNext()) {
            Statement stmt = stmts.nextStatement();

            Property predicate = stmt.getPredicate();
            if (predicate.getNameSpace().equals(bfNamespace)) {
                Property ld4lProp = model.createProperty(
                        ld4lNamespace, predicate.getLocalName());
                assertions.add(stmt.getSubject(), ld4lProp, stmt.getObject());
                stmts.remove();
            }   
        }
        
        model.add(assertions);    
    }


}
