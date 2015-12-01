package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.graph.NodeFactory;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
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

public class BfTitleConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTitleConverter.class);

    
    private static final List<BfType> TYPES_TO_CONVERT = 
            new ArrayList<BfType>();
    static {
        TYPES_TO_CONVERT.add(BfType.BF_TITLE);
    }
    
    private static final List<BfType> TYPES_TO_RETRACT = 
            new ArrayList<BfType>();
    static {

    }
    
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_INSTANCE_TITLE);
        // PROPERTIES_TO_CONVERT.add(BfProperty.BF_TITLE_VALUE);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_WORK_TITLE);
    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {

    }
    
    
    public BfTitleConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    
    @Override
    protected void convertModel() {
        
      convertTitleValue();

      model.remove(retractions);

      super.convertModel();
    }
    
    private void convertTitleValue() {
        
        Statement statement = 
                subject.getProperty(BfProperty.BF_TITLE_VALUE.property());
        if (statement != null) {
            Literal literal = statement.getLiteral();
            retractions.add(statement);
            assertions.add(subject, RDFS.label, 
                    normalizeTitle(literal));
            // return literal?
        }
        
        // TODO Parse label (titleValue) into madsrdf sub-elements insofar 
        // as possible.
        // assertions.add(createTitleElements());
    }
    
    private Literal normalizeTitle(Literal literal) {
        
        if (literal == null) {
            return null;
        }
        String language = literal.getLanguage();
        String value = literal.getLexicalForm();
        // OK if language is null or empty
        return model.createLiteral(normalizeTitle(value), language);
    }
    
    static String normalizeTitle(String title) {
        // Should we use NacoNormalizer.normalize()?    
        String normalizedTitle = title.replaceAll("\\.+\\s*$", "")
                .replaceAll(";\\s*$", "")
                .replaceAll("\\s*$", ""); 
        return normalizedTitle;
    }

    public Model create(Resource subject, Literal label) {
        
        Model model = ModelFactory.createDefaultModel();
        Resource title = 
                model.createResource(RdfProcessor.mintUri(localNamespace));
        model.add(title, RDF.type, Ld4lType.TITLE.ontClass());        
        model.add(title, RDFS.label, label);
        model.add(subject, Ld4lProperty.HAS_TITLE.property(), title);
        // model.add(createTitleElements(label));
        return model;
    }
    
    private Model createTitleElements(Literal title) {
        // If we can parse the title string into sub-elements, do so here.
        // E.g., if title starts with an article, create a NonSortTitleElement.
        Model model = ModelFactory.createDefaultModel();
        return model;
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
