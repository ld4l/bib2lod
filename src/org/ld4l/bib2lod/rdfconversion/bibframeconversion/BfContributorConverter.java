package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class BfContributorConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfContributorConverter.class);
    
    
    private static final List<BfType> TYPES_TO_CONVERT = 
            new ArrayList<BfType>();
    static {
 
    }
    
    private static final List<BfType> TYPES_TO_RETRACT = 
            new ArrayList<BfType>();
    static {

    }

    private static final Map<BfProperty, Ld4lType> PROPERTY_TO_TYPE = 
            new HashMap<BfProperty, Ld4lType>();
    static {
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_CREATOR, Ld4lType.CREATOR_CONTRIBUTION);
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_CONTRIBUTOR, Ld4lType.CONTRIBUTION);
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_RELATOR, Ld4lType.CONTRIBUTION);
        PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_AUTHOR, Ld4lType.AUTHOR_CONTRIBUTION);
        PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_COMPOSER, Ld4lType.COMPOSER_CONTRIBUTION);
        PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_CONDUCTOR, Ld4lType.CONDUCTOR_CONTRIBUTION);
        PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_EDITOR, Ld4lType.EDITOR_CONTRIBUTION);
        PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_NARRATOR, Ld4lType.NARRATOR_CONTRIBUTION);
        PROPERTY_TO_TYPE.put(
                BfProperty.RELATORS_PERFORMER, Ld4lType.PERFORMER_CONTRIBUTION);
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
 
    protected BfContributorConverter(
            String localNamespace, Statement statement) {
        super(localNamespace, statement);
    } 
    
    @Override
    protected void convertModel() {
        
        createContribution();
      
      // If there were retractions, we should apply them to the model here,
      // so that super.convertModel() doesn't reprocess them.
      
      super.convertModel();
    }

    private void createContribution() {
        
        // Linking statement = :work <contributorPredicate> :agent
        // where contributorPredicate is one of PROPERTY_TO_TYPE.keySet()
        // and :agent is a bf:Agent type (bf:Person, bf:Organization, etc.)
        Resource work = linkingStatement.getSubject();
        Property contributorProp = linkingStatement.getPredicate();
        Resource agent = linkingStatement.getResource();
        
        LOGGER.debug(contributorProp.getURI());
        BfProperty bfProp = BfProperty.get(contributorProp);

        // TODO Generate a more specific creator type based on the type of 
        // work. I.e., in :work bf:creator :agent 
        // if :work is a bf:Text, then create an AuthorContribution; if
        // :work is bf:NotatedMusic, create a ComposerContribution, etc.
        Ld4lType newType = PROPERTY_TO_TYPE.containsKey(bfProp) ? 
                PROPERTY_TO_TYPE.get(bfProp) : Ld4lType.CONTRIBUTION;

        Resource contribution = 
                assertions.createResource(RdfProcessor.mintUri(localNamespace));
        assertions.add(contribution, RDF.type, newType.ontClass());
        
        if (newType.label() != null) {
            assertions.add(contribution, RDFS.label, newType.label());
        }
        
        assertions.add(contribution, Ld4lProperty.HAS_AGENT.property(), agent);
        
        assertions.add(
                work, Ld4lProperty.HAS_CONTRIBUTION.property(), contribution);
        retractions.add(linkingStatement);
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