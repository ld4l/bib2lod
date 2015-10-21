package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfPersonConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfPersonConverter.class);
    
    private static final String URI_POSTFIX = "bfPerson";
    
    public BfPersonConverter(OntType type) {
        super(type);
    }
    
    // May want to pass subject and inputModel to constructor instead, to store
    // in instance field.
    public Model convert(Resource subject, Model model) {
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Before converson");
            printModel(model);
        }
        
        Model assertions = ModelFactory.createDefaultModel();
        Model retractions = ModelFactory.createDefaultModel();

        // Change the type from bf:Person to foaf:Person
        assertions.add(subject, RDF.type, 
                model.createResource(OntType.FOAF_PERSON.uri()));
        retractions.add(subject, RDF.type, 
                model.createResource(OntType.BF_PERSON.uri()));

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("After changing type from bf:Person to foaf:Person");
            printModel(model);
        }
        
        // Create a new foaf:Person with the original URI of the bf:Person. This
        // will maintain relationships from, e.g., a bf:Work to the a 
        // foaf:Person where originally the relationship was to a bf:Person.
        // TODO ****************************************************************
        // Is it really going to be this simple?? Or do we need to maintain
        // a map from Bibframe individuals to new RWO individuals and manually
        // make the substitutions in the RDF? This would require a second pass 
        // over the files once the first pass has created all the RWO URIs.
        // In that case, leave the original URI of the bf:Person alone, and mint
        // a new one for the foaf:Person.
        // *********************************************************************

 

        
        // TODO Not sure if we should also add the inverse? We could either 
        // leave the inverse out altogether, or apply reasoning as a general 
        // conversion step by using an inferencing model based on the LD4L 
        // ontology.
//        addStatement(bfPerson, OntProperty.MADSRDF_IDENTIFIES_RWO,
//                 foafPerson, assertions);


        

        
        model.add(assertions);
        model.remove(retractions);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("After applying assertions and retractions:");
            printModel(model);
        }
        
        // TODO make sure if no statements altered we return the input model...
        // if that ever happens?
        return model;
    

    }
    

    

}
