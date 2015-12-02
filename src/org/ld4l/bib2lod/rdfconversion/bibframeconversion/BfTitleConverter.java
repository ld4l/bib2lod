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
        
        Statement titleValueStmt = 
                subject.getProperty(BfProperty.BF_TITLE_VALUE.property());
        if (titleValueStmt != null) {

            retractions.add(titleValueStmt);
            Literal titleValueLiteral = titleValueStmt.getLiteral();
            
            Statement subtitleStmt = 
                    subject.getProperty(BfProperty.BF_SUBTITLE.property());            
            if (subtitleStmt != null) {
                retractions.add(subtitleStmt);
                assertions.add(convertTitleValueAndSubtitle(titleValueLiteral, 
                        subtitleStmt.getLiteral()));
           
            } else {
            
                assertions.add(subject, RDFS.label, 
                        normalizeTitle(titleValueLiteral));
            }

        }
        
        // TODO Parse label into madsrdf sub-elements insofar 
        // as possible.
        // assertions.add(createTitleElements());
    }

    /*
     * If there's a subtitle, the titleValue reflects only the main 
     * title, not the entire title. Concatenate subtitle and titleValue
     * to get rdfs:label, and create SubTitleElement and 
     * MainTitleElement.
     */
    private Model convertTitleValueAndSubtitle(Literal titleValueLiteral, 
            Literal subtitleLiteral) {
        
        Model model = ModelFactory.createDefaultModel();
        
        String mainTitle = titleValueLiteral.getString();
        String subtitle = subtitleLiteral.getString();
                
        String label = normalizeTitle(mainTitle + " " + subtitle);
        // Use the titleValue language as the full label language
        model.add(subject, RDFS.label, label, 
                titleValueLiteral.getLanguage());

        model.add(createTitleElement(Ld4lType.MAIN_TITLE_ELEMENT, 
                titleValueLiteral));
        model.add(createTitleElement(Ld4lType.SUBTITLE_ELEMENT,
                 subtitleLiteral)); 
        
        return model;
    }
    
    private Model createTitleElement(Ld4lType titleElementType, 
            Literal literal) {
        
        Model model = ModelFactory.createDefaultModel();
        Resource titleElement = 
                model.createResource(RdfProcessor.mintUri(localNamespace));
        String normalizedString = normalizeTitle(literal.getLexicalForm());
        model.add(subject, Ld4lProperty.HAS_PART.property(), titleElement);
        model.add(titleElement, RDFS.label, normalizedString, 
                literal.getLanguage());
        model.add(titleElement, RDF.type, titleElementType.ontClass());
        
        return model;            
    }
    
    private Literal normalizeTitle(Literal literal) {
        
        if (literal == null) {
            return null;
        }
        // Create a new literal with a normalized string value and the language
        // of the original literal.
        String value = normalizeTitle(literal.getLexicalForm());

        String language = literal.getLanguage();
        // OK if language is null or empty
        return literal.getModel().createLiteral(value, language); 
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
        // Call createTitleElement for each piece of the title identified.
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
