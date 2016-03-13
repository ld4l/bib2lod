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

public class BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfResourceConverter.class);

    // Default resource submodel consists of all the statements in which the 
    // resource is either the subject or the object. Subclasses may define a 
    // more complex query.
    private static ParameterizedSparqlString RESOURCE_SUBMODEL_PSS = 
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
    protected List<Resource> resourcesToRemove;
    
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

        // Initialize instance variables for processing of new subject
        init(subject);

        convert();
        
        this.subject.getModel().close();
        
        return outputModel;
    }
    
    private void init(Resource subject) {
        this.subject = getResourceWithSubModel(subject);               
        this.outputModel = ModelFactory.createDefaultModel();
        this.resourcesToRemove = new ArrayList<Resource>();       
    }

    protected Resource getResourceWithSubModel(Resource subject) {
        Model resourceSubModel = getResourceSubModel(subject);
        
        // Get the resource in the submodel with the same URI as the input
        // subject, so that references to subject.getModel() will return the
        // submodel rather than the full input model.
        // NB Model.createResource() may return an existing resource rather
        // than creating a new one.
        return resourceSubModel.createResource(subject.getURI());        
    }
    
    // TODO Consider whether this is even needed. The input models are not very
    // large, assuming a small input file size. Does the time taken to build
    // the submodel offset the gain of faster select uriGeneratorClass? This
    // approach also introduces complexity: (a) constructing the type-specific
    // submodel, and (b) converters don't have direct access to the original
    // input model to delete statements - though it could, of course, be saved
    // by them as the input model of the resource they are passed, before they
    // create the submodel and new resource. On the other hand, we may not want 
    // to rely on input file sizes being below a specific threshold value.
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
        
//        RdfProcessor.printModel(resourceSubModel, 
//                "Submodel for resource " + resource.getURI());
        
        return resourceSubModel;    
    }
    
    protected ParameterizedSparqlString getResourceSubModelPss() {
        return RESOURCE_SUBMODEL_PSS;
    }
        
    /* 
     * Default conversion method. Subclasses may override.
     */
    protected Model convert() {

        // Map of Bibframe to LD4L types.
        Map<Resource, Resource> typeMap = getTypeMap();
        
        // Map of Bibframe to LD4L properties.
        Map<Property, Property> propertyMap = getPropertyMap();

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
                outputModel.add(
                        stmtSubject, propertyMap.get(predicate), object);                
            }
        }
        
        return outputModel;  
    }   

    // Subclasses may override for non-default mappings or to remove types
    // that shouldn't be asserted.
    protected Map<Resource, Resource> getTypeMap() {
        
        // WRONG - alters map returned by BfType.typeMap()
        // Map<Resource, Resource> typeMap = BfType.typeMap();
        // typeMap.putAll(getTypeMap());        
        Map<Resource, Resource> typeMap = new HashMap<Resource, Resource>();
        
        // Get default mapping from Bibframe to LD4L properties
        typeMap.putAll(BfType.typeMap());
        
        return typeMap;
    }
    
    // Default. Subclasses may override.
    protected Map<Property, Property> getPropertyMap() {
        
        // WRONG - alters map returned by BfProperty.propertyMap()
        // Map<Property, Property> propertyMap = BfProperty.propertyMap();      
        // propertyMap.putAll(getPropertyMap());
        Map<Property, Property> propertyMap = new HashMap<Property, Property>();
        
        // Get default mapping from Bibframe to LD4L properties
        propertyMap.putAll(BfProperty.propertyMap());
        
        return propertyMap;
    }
    
    public List<Resource> getResourcesToRemove() {
        return resourcesToRemove;
    }
    
    // When this converter determines that another resource should be removed,
    // add it to the list, and remove all statements pertaining to the resource
    // from the subject submodel. The list resourcesToRemove will be used to
    // remove a resource during the resource iteration in BibframeConverter.
    protected void removeResource(Resource resource) {            
        Model model = subject.getModel();
        
        model.removeAll(resource, null, null);
        model.removeAll(null, null, resource);
        
        resourcesToRemove.add(resource);
        
        LOGGER.debug("Adding resource to remove: " + resource.getURI());
    }
  
}