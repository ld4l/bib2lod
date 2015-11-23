package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class BfProviderConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfProviderConverter.class);

    
    private static final Map<BfProperty, Ld4lType> PROPERTY_TO_TYPE = 
            new HashMap<BfProperty, Ld4lType>();
    static {
        PROPERTY_TO_TYPE.put(BfProperty.BF_PUBLICATION, 
                Ld4lType.PUBLISHER_PROVISION);
        PROPERTY_TO_TYPE.put(BfProperty.BF_DISTRIBUTION, 
                Ld4lType.DISTRIBUTOR_PROVISION);
    }

    private static final List<BfType> TYPES_TO_CONVERT = 
            new ArrayList<BfType>();
    static {
 
    }
    
    private static final List<BfType> TYPES_TO_RETRACT = 
            new ArrayList<BfType>();
    static {
        
    }
    
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {

    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {

    }

    public BfProviderConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    @Override
    protected Model convertSubject(Resource subject) {
        
        this.subject = subject;
        this.model = subject.getModel();
       
        
        assertions = ModelFactory.createDefaultModel();
        
        // So far we are not using the retractions model, using 
        // StmtIterator.remove() instead, but declaring this gives added
        // flexibility to the subclasses.
        retractions = ModelFactory.createDefaultModel();
        
        convertModel();
        
        model.add(assertions)
             .remove(retractions);
        
        return model;
        
    }
    
    @Override
    protected void convertModel() {
        
      StmtIterator stmts = subject.listProperties();
      while (stmts.hasNext()) {
          
          Statement statement = stmts.nextStatement();
          Property predicate = statement.getPredicate();
        
          if (predicate.equals(RDF.type)) {
              assertions.add(subject, RDF.type, 
                      Ld4lType.PUBLISHER_PROVISION.ontClass());
              assertions.add(subject, RDFS.label, "Publisher");
              
          } else if (predicate.equals(
                  BfProperty.BF_PROVIDER_NAME.property())) {
              assertions.add(subject, Ld4lProperty.AGENT.property(), 
                      statement.getResource());
              
          } else if (predicate.equals(
                  BfProperty.BF_PROVIDER_PLACE.property())) {
              assertions.add(subject, Ld4lProperty.AT_LOCATION.property(), 
                      statement.getResource());
          
          } else if (predicate.equals(
                  BfProperty.BF_PROVIDER_DATE.property())) {
              assertions.add(subject, Ld4lProperty.DATE.property(), 
                      statement.getLiteral());
          }
          
          stmts.remove();
      }
    }

    @Override 
    protected List<BfType> getBfTypesToConvert() {
        return TYPES_TO_CONVERT;
    }
    
    @Override 
    protected List<BfType> getBfTypesToRetract() {
        return TYPES_TO_RETRACT;
    }
    
    @Override
    protected List<BfProperty> getBfPropertiesToConvert() {
        return PROPERTIES_TO_CONVERT;
    }
   
    @Override
    protected Map<BfProperty, Ld4lProperty> getBfPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected List<BfProperty> getBfPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }
}
