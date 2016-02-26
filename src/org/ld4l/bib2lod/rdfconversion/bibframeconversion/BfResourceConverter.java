package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.query.Query;
import org.apache.jena.query.QueryExecution;
import org.apache.jena.query.QueryExecutionFactory;
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
import org.ld4l.bib2lod.rdfconversion.OntNamespace;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);
    
    // Default resource submodel consists of all the statements in which the 
    // resource is either the subject or the object. Subclasses may define a 
    // more complex query.
    private static ParameterizedSparqlString resourceSubModelPss = 
            new ParameterizedSparqlString(
                    "CONSTRUCT { ?resource ?p1 ?o . "
                    + " ?s ?p2 ?resource . "                
                    + "} WHERE {  { "                                                      
                    + "?resource ?p1 ?o . "
                    + "} UNION { "
                    + "?s ?p2 ?resource . "
                    + "} } ");


    protected String localNamespace;
    protected Resource subject;
    protected Model outputModel;
    
    // This would be the full input model for the file. Pass it in if we need
    // more data than we got in the subject submodel. 
    // protected Model inputModel;

    public BfResourceConverter(String localNamespace) {
        this.localNamespace = localNamespace;
    }

    /*
     * Public interface method. 
     */
    public final Model convert(Resource subject) {         

        // Initialize converter for processing of new subject
        Model subjectSubModel = getResourceSubModel(subject);
        
        // Get the resource in the submodel with the same URI as the input
        // subject, so that references to subject.getModel() will return the
        // submodel rather than the full input model.
        // NB Model.createResource() may return an existing resource rather
        // than creating a new one.
        this.subject = subjectSubModel.createResource(subject.getURI());
        
        // Start with a new output model for each subject conversion.
        this.outputModel = ModelFactory.createDefaultModel();
        
        convert();
        
        subjectSubModel.close();
        
        return outputModel;
    }
 
    /* 
     * Default conversion method. Subclasses may override.
     */
    protected Model convert() {

        // Map of Bibframe to LD4L types.
        Map<Resource, Resource> typeMap = buildTypeMap();
        
        // Map of Bibframe to LD4L properties.
        Map<Property, Property> propertyMap = buildPropertyMap();

        StmtIterator stmts = subject.getModel().listStatements();
            
        while (stmts.hasNext()) {
            Statement stmt = stmts.nextStatement();
            Property predicate = stmt.getPredicate();
            RDFNode object = stmt.getObject();
            
            // The subjectModel may contain statements with other subjects; for
            // example, statements where it is the object.
            Resource stmtSubject = stmt.getSubject();
            if (LOGGER.isDebugEnabled()) {
                if (! stmtSubject.equals(subject)) {
                    LOGGER.debug("Found statement " + stmt.toString() 
                            + " where subject is not " + this.subject.getURI());
                }
            }
     
            if (predicate.equals(RDF.type)) {

                Resource type = object.asResource();                   
                if (typeMap.containsKey(type)) {
                    outputModel.add(stmtSubject, RDF.type, typeMap.get(type));
                }

            } else if (propertyMap.containsKey(predicate)) {               
                outputModel.add(stmtSubject, propertyMap.get(predicate), object);                
            }
        }
        
        return outputModel;  
    }   

    protected ParameterizedSparqlString getResourceSubModelPss() {
        return resourceSubModelPss;
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
        
        // WRONG - alters map returned by BfType.typeMap()
        // Map<Resource, Resource> typeMap = BfType.typeMap();
        // typeMap.putAll(getTypeMap());
        
        Map<Resource, Resource> typeMap = new HashMap<Resource, Resource>();
        // Get default mapping from Bibframe to LD4L properties
        typeMap.putAll(BfType.typeMap());
        // Type-specific mappings override the default mapping
        typeMap.putAll(getTypeMap());
        
        // If a child converter needs to delete rather than convert a type,
        // remove it from the map. For example, FAST topics remove the 
        // relationship to a madsrdf:Authority, whereas it is retained by 
        // default.
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
        
        // WRONG - alters map returned by BfProperty.propertyMap()
        // Map<Property, Property> propertyMap = BfProperty.propertyMap();      
        // propertyMap.putAll(getPropertyMap());

        Map<Property, Property> propertyMap = new HashMap<Property, Property>();
        
        // Get default mapping from Bibframe to LD4L properties
        propertyMap.putAll(BfProperty.propertyMap());
        // Type-specific mapping override the defaults
        propertyMap.putAll(getPropertyMap());

        // If a child converter needs to delete rather than convert a property,
        // remove it from the map. 
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

    // TODO Consider whether this is even needed. The input models are not very
    // large, assuming a small input file size. Does the time taken to build
    // the submodel offset the gain of faster select queries? On the other hand,
    // we may not want to depend on input file sizes below a threshold.
    // 
    protected Model getResourceSubModel(Resource resource) {

        LOGGER.debug("Getting resource submodel for " + resource.getURI());
        
        ParameterizedSparqlString pss = getResourceSubModelPss();
        pss.setNsPrefix(OntNamespace.BIBFRAME.prefix(),
                OntNamespace.BIBFRAME.uri());
        pss.setIri("resource", resource.getURI());
        
        Query query = pss.asQuery();
        LOGGER.debug(query.toString());
        QueryExecution qexec = QueryExecutionFactory.create(
                query, resource.getModel());
        Model resourceSubModel =  qexec.execConstruct();
        qexec.close();
        
        RdfProcessor.printModel(resourceSubModel, 
                "Submodel for resource " + resource.getURI());
        
        return resourceSubModel;    
    }
   
}