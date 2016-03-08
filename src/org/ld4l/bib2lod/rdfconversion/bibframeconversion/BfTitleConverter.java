package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lIndividual;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class BfTitleConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTitleConverter.class);
    
    // In addition to the property linking the bib resource to the title, 
    // i.e. bf:workTitle or bf:instanceTitle, get any datatype title property
    // assertions for the bib resource, i.e., bf:title or bf:titleStatement.
    private static ParameterizedSparqlString RESOURCE_SUBMODEL_PSS = 
            new ParameterizedSparqlString(
            "CONSTRUCT { ?bib ?bibProp ?resource . "
            + "?resource ?titleProp ?titlePropObj . "
            + "?bib ?bibDataProp ?dataVal . } "
            + "WHERE { { "
            + "?bib ?bibProp ?resource . "
            + "OPTIONAL { "
            + "?bib ?bibDataProp ?dataVal . } "
            + "FILTER (?bibDataProp = " + BfProperty.BF_TITLE.sparqlUri() + ") "
            + "} UNION { "
            + "?resource ?titleProp ?titlePropObj . "
            + "} }");
    
    
    private static final List<String> NON_SORT_STRINGS = 
            new ArrayList<String>();
    static {
        // Language-dependent. Only handling English for now. However, bf:title
        // with language x-bf-sort also helps identify a non-sort string.
        NON_SORT_STRINGS.add("A ");
        NON_SORT_STRINGS.add("The ");
    }
    
    public BfTitleConverter(String localNamespace) {
        super(localNamespace);
    }
    
    @Override
    protected ParameterizedSparqlString getResourceSubModelPss() {
        LOGGER.debug(RESOURCE_SUBMODEL_PSS);
        return RESOURCE_SUBMODEL_PSS;
    }
    
    /* 
     * Static methods designed for use both by BfTitleConverter and by 
     * BfInstanceConverter / BfWorkConverter, where we have no Title object.
     */
    
    // bf:titleStatement is used only with an Instance. It indicates a 
    // transcribed title. This is expressed in LD4L with the 
    // ld4l:hasSourceStatus predicate.
    protected static Model convertBfTitleStatement(
            Resource instance, Literal value, String localNamespace) {

        Resource title = createTranscribedTitle(value, localNamespace);
        Model titleModel = title.getModel();
        titleModel.add(instance, Ld4lProperty.HAS_TITLE.property(), title);
        return titleModel;
    }
    
    private static Resource createTranscribedTitle(
            Literal titleValue, String localNamespace) {
        
        // TODO Here we ignore the question of whether the titleStatement value
        // should be parsed into NonSortTitleElement and MainTitleElement.
        Resource title = createTitle(titleValue, localNamespace);
        Model model = title.getModel();
        model.add(title, Ld4lProperty.HAS_SOURCE_STATUS.property(), 
                Ld4lIndividual.SOURCE_STATUS_TRANSCRIBED.individual());
        return title;      
    }

    // This is the simplest createTitle method: there are no title subparts
    // other than the MainTitleElement.
    private static Resource createTitle(
            Literal titleValue, String localNamespace) {
        
        // Normalize the title string value
        Literal normalizedTitleValue = normalizeTitle(titleValue);
        
        // Create the Title 
        Model model = ModelFactory.createDefaultModel();
        Resource title = 
                model.createResource(RdfProcessor.mintUri(localNamespace)); 
        model.add(title, RDF.type, Ld4lType.TITLE.type());
        model.add(title, RDFS.label, normalizedTitleValue);

        // TODO Analyze the Title value for a NonSortElement too? In that case,
        // MainTitleElement is the title value minus the NonSortElement value.
        
        // Create the MainTitleElement; all Titles have at least this element.
        Resource mainTitleElement = 
                createTitleElement(Ld4lType.MAIN_TITLE_ELEMENT, 
                        normalizedTitleValue, localNamespace, false);
                        
        model.add(mainTitleElement.getModel());
        model.add(title, Ld4lProperty.HAS_PART.property(), mainTitleElement);

        return title;
    }
    


    private static Resource createTitleElement(
            Ld4lType titleElementType, Literal titleValue, 
            String localNamespace, boolean normalize) {

        Model model = ModelFactory.createDefaultModel();
        
        // If the literal was not already normalized in a previous step, 
        // normalize it now.
        if (normalize) {
            titleValue = normalizeTitle(titleValue);
        }
        
        Resource titleElement = 
                model.createResource(RdfProcessor.mintUri(localNamespace));
        model.add(titleElement, RDF.type, titleElementType.type());
        model.add(titleElement, RDFS.label, titleValue);

        return titleElement;                  
    }
    
    private static Literal normalizeTitle(Literal title) {
        
        String language = title.getLanguage();
        String text = title.getLexicalForm();
        String normalizedText = normalizeTitle(text);
        Literal normalizedTitle = 
                ResourceFactory.createLangLiteral(normalizedText, language);
        return normalizedTitle;
    }

    private static String normalizeTitle(String title) {
        /*
         * Remove final period, final spaces, final ellipsis
         * Final ellipsis occurs in rare cases when the bf:label contains title 
         * plus author, separated by ellipsis. The LC converter splits these 
         * and retains the ellipsis in the titleValue.
         * Examples:
         <http://ld4l.library.cornell.edu/individual/2235title31> <http://bibframe.org/vocab/label> "Old Orange Houses ...  by Mildred Parker Seese." . 
         <http://ld4l.library.cornell.edu/individual/2235title31> <http://bibframe.org/vocab/titleValue> "Old Orange Houses ..." . 
        
         <http://ld4l.library.cornell.edu/individual/2537title132> <http://bibframe.org/vocab/label> "A manual of veterinary sanitary science and police ... / by George Fleming." . 
         <http://ld4l.library.cornell.edu/individual/2537title132> <http://bibframe.org/vocab/titleValue> "A manual of veterinary sanitary science and police ..." .               
        */
        return title.replaceAll("[\\s.]+$", "");
    }
    

    
    protected static Model convertBfTitleProp(Resource bibResource) {
        Model model = ModelFactory.createDefaultModel();
        
        return model;
    }
    
    /**
     * Return the Literal object of bf:title.
     * @param resource
     * @return
     */
    protected static Literal getBfTitleLiteral(Resource resource) {
        Literal title = null;
        
        
        return title;
    }
    
    // Used in two cases: (1) processing the Work/Instance bf:title sort title.
    // (2) processing the Title entity when the Work/Instance has a bf:title
    // sort title.
    protected static String getSortTitle(Resource bibResource) {

      StmtIterator stmts = 
              bibResource.listProperties(BfProperty.BF_TITLE.property());
      while (stmts.hasNext()) {
          Literal title = stmts.next().getLiteral();
          if (isSortTitle(title)) {
              return title.getLexicalForm();
          }
      }
      
      return null;
    }

    /**
     * Return true iff the title literal is a sort title.
     * @param title
     * @return
     */
    private static boolean isSortTitle(Literal title) {

        String language = title.getLanguage();
        return (language != null && language.equals("x-bf-sort"));       
    }
    
//    private String getSortTitle() {
//
//            
//            if (nonSortElement != null) {
//                // Remove the non-sort string from the full title string to get the 
//                // main title element string 
//                String nonSortString = 
//                        nonSortElement.getProperty(RDFS.label).getString();
//                mainTitleString = mainTitleString.substring(nonSortString.length());
//            }
//            
//            return mainTitleString;
//        }
//      }
    

    



    
    public Model convert() {
        
        Model model = subject.getModel();
       
        Resource bibResource = getRelatedBibResource();

//       
//
//        StmtIterator stmts = subject.getModel().listStatements();
//        while (stmts.hasNext()) {
//            Statement statement = stmts.nextStatement();
//            
//            Resource stmtSubject = statement.getSubject();
//            Property property = statement.getPredicate();
//            RDFNode object = statement.getObject();
//            BfProperty bfProp = BfProperty.get(property);
//            
//            if (bfProp.equals(BfProperty.BF_TITLE_VALUE) {
//                convertTitleValue();
//                
//            } //else if (bfProp.equals)
//        }
        return super.convert();
    }
   
    /* 
     * Get the Work or Instance of which this Title is the title.
     */
    private Resource getRelatedBibResource() {

        Resource bibResource = null;
        Model model = subject.getModel();
        ResIterator resources = model.listResourcesWithProperty(null, subject);
        while (resources.hasNext()) {
            // There can be only one
            bibResource = resources.nextResource();
        }
        return bibResource;    
    }
    
    private void convertTitleValue() {
        
        Statement titleValueStmt = 
                subject.getProperty(BfProperty.BF_TITLE_VALUE.property());
        
        if (titleValueStmt != null) {

            // bf:titleValue string and language
            String titleValueString = 
                    normalizeTitle(titleValueStmt.getString());
            String titleValueLanguage = titleValueStmt.getLanguage();
            
            // This will be the rdfs:label of the Title
            String fullTitleString = titleValueString;
       
            Resource nonSortElement = createNonSortTitleElement(
                    titleValueString, titleValueLanguage, subject, outputModel);

            // This will be the rdfs:label value of the MainTitleElement. In
            // the absence of other title elements, it's the full bf:titleValue
            // string. If there's a non-sort element, remove it from the full title
            // title string to get the main title element string.         
//            String mainTitleString = 
//                    removeNonSortString(titleValueString, nonSortElement);
               
//            Resource mainTitleElement = 
//                    createTitleElement(Ld4lType.MAIN_TITLE_ELEMENT, 
//                            mainTitleString, titleValueLanguage);
            
            // Non-sort element precedes main title element
//            if (nonSortElement != null) {
//                outputModel.add(nonSortElement, Ld4lProperty.PRECEDES.property(), 
//                        mainTitleElement);
//            }
            
            Resource subtitleElement = null;
                         
            Statement subtitleStmt = 
                    subject.getProperty(BfProperty.BF_SUBTITLE.property());            
            if (subtitleStmt != null) {

                // Create the subtitle element
                String subtitleValue = normalizeTitle(subtitleStmt.getString());
                String language = subtitleStmt.getLanguage();
//                subtitleElement = createTitleElement(
//                        Ld4lType.SUBTITLE_ELEMENT, subtitleValue, language);
                
                // When there's a subtitle, the titleValue contains only the 
                // main title, not the full title. The rdfs:label (full title)
                // concatenates both main title and subtitle strings.
                fullTitleString += " " + subtitleValue;
                
                // Order the mainTitleElement and subtitleElement
//                outputModel.add(
//                        mainTitleElement, Ld4lProperty.PRECEDES.property(), 
//                        subtitleElement);
          
            } 
            
            // Add the rdfs:label of the Title
            outputModel.add(
                    subject, RDFS.label, fullTitleString, titleValueLanguage);
            
            // NB Some Title objects have both a bf:titleValue and a bf:label, 
            // with different strings. We'll end up assigning two rdfs:labels.
            // Ideally we should not have two rdfs:labels, but it's not clear
            // how to resolve this case. In at least some cases the bf:label
            // has extraneous, non-title text, but it's not clear whether that's
            // always the case. E.g., record 2083918 has
            // title5 bf:label "Hans Bethe papers, [ca. 1931]-1992."
            // title5 bf:titleValue "Hans Bethe papers,"
            
            // TODO Part name/number elements
        }
    }
    
    private Resource createNonSortTitleElement(String titleString, 
            String titleLanguage, Resource title, Model model) {
        
        Resource nonSortElement = null;
        
        // Create non-sort element from initial definite/indefinite article
        for (String nonSortString : NON_SORT_STRINGS) {
            
            if (titleString.startsWith(nonSortString)) {
                
//                nonSortElement = 
//                        createTitleElement(Ld4lType.NON_SORT_TITLE_ELEMENT, 
//                                nonSortString, titleLanguage, title, model);
      
                break;                 
            } 
        }
        
        return nonSortElement;
    }

    

    
//  private Resource createTitleElement(Ld4lType titleElementType, 
//          Literal literal) {
//      return createTitleElement(titleElementType, literal, false);
//  }
//  
//  private Resource createTitleElement(Ld4lType titleElementType, 
//          Literal literal, boolean normalize) {
//
//      String value = literal.getLexicalForm();
//      if (normalize) {
//          value = normalizeTitle(value);
//      }
//      return createTitleElement(titleElementType, value, 
//              literal.getLanguage());        
//  }
    


//  private Literal normalizeTitle(Literal literal) {
//  
//  if (literal == null) {
//      return null;
//  }
//  // Create a new literal with a normalized string value and the language
//  // of the original literal.
//  String value = normalizeTitle(literal.getLexicalForm());
//
//  String language = literal.getLanguage();
//  
//  // OK if language is null or empty
//  return ResourceFactory.createLangLiteral(value, language); 
//}
    
    public Model create(Resource subject, Literal titleStatementLiteral) {
        
        Model model = ModelFactory.createDefaultModel();
        Resource title = 
                model.createResource(RdfProcessor.mintUri(localNamespace));
        model.add(title, RDF.type, Ld4lType.TITLE.type());        
        model.add(subject, Ld4lProperty.HAS_TITLE.property(), title);
        
        String titleStatementString = 
                normalizeTitle(titleStatementLiteral.getLexicalForm());
        
        String language = titleStatementLiteral.getLanguage();
      
        // The full bf:titleStatement string is the rdfs:label of the title
        model.add(title, RDFS.label, titleStatementString);
        
        Resource nonSortElement = 
                createNonSortTitleElement(
                        titleStatementString, language, title, model);

        // This will be the rdfs:label value of the MainTitleElement. In
        // the absence of other title elements, it's the full bf:titleStatement
        // string. If there's a non-sort element, remove it from the full title
        // string to get the main title element string.
//        String mainTitleString = 
//                removeNonSortString(titleStatementString, nonSortElement);
       
        // TODO Perhaps try to parse into subtitle and part name elements - but 
        // seems very error-prone.
           
//        Resource mainTitleElement = 
//                createTitleElement(Ld4lType.MAIN_TITLE_ELEMENT, 
//                        mainTitleString, language, title, model);
        
        // Non-sort element precedes main title element
//        if (nonSortElement != null) {
//            model.add(nonSortElement, Ld4lProperty.PRECEDES.property(), 
//                    mainTitleElement);
//        }

        return model;
    }
    


    
}
