package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

/**
 * Provides a set of utilities for use in converting Bibframe title properties
 * and Title entities. These methods are called by BfWorkConverter, 
 * BfInstanceConverter, and BfTitleConverter.
 */

public final class TitleUtils {

    private static final Logger LOGGER = 
            LogManager.getLogger(TitleUtils.class);
    
    private static final List<String> NON_SORT_STRINGS = 
            new ArrayList<String>();
    static {
        // Language-dependent. Only handling English for now, and might misfire
        // on other languages. An accompanying bf:title value with language 
        // value "x-bf-sort" also identifies a non-sort string; but this is not
        // always present.
        NON_SORT_STRINGS.add("An ");
        NON_SORT_STRINGS.add("A ");
        NON_SORT_STRINGS.add("The ");
    }
    
    private TitleUtils() { }

    /*
     * Used by BfWorkConverter and BfInstanceConverter to create a new
     * Title object from one or more bf:title datatype properties.
     * 
     * Convert any bf:title statements attached to the Work or Instance, 
     * including sort title values. A sort title accompanies another title,
     * expressed as either the object of another bf:title statement, or as a
     * bf:Title. Here we handle the former. A sort title never occurs 
     */
    
    // TODO **** break into separate methods - both because the method is too 
    // long, and because some of it will also be used by BfTitleConverter; 
    // specifically, the non-sort title element creation.

    static Model convertBfTitleDataProp(
            Resource bibResource, String localNamespace) {
        
        Literal bfSortTitleLiteral = null;
        Literal bfTitleLiteral = null;
        StmtIterator statements = 
                bibResource.listProperties(BfProperty.BF_TITLE.property());
        while (statements.hasNext()) {
            Statement statement = statements.nextStatement();
            Literal literal = statement.getLiteral();
            if (isSortTitle(literal)) {
                bfSortTitleLiteral = literal; 
            } else {
                bfTitleLiteral = literal;
            }
        }

        if (bfTitleLiteral == null) {
            return null;
        }

        bfTitleLiteral = normalizeTitle(bfTitleLiteral);
        String titleLabelString = bfTitleLiteral.getLexicalForm();
        LOGGER.debug("title label: \"" + titleLabelString + "\"");
        String titleLabelLanguage = bfTitleLiteral.getLanguage();
        
        Resource title = createTitle(bfTitleLiteral, localNamespace, false);
        Model titleModel = title.getModel();
        titleModel.add(bibResource, Ld4lProperty.HAS_TITLE.property(), title);

        // TODO *** When we're converting a bf:Title:
        // But this isn't true if there's a subtitle or part name element.
        // FIRST isolate the main title element from these. THEN remove the
        // non-sort string and create the NonSortTitleElement
        String mainTitleString = titleLabelString;

        // Now: when we're dealing with a bf:Title rather than a bf:title:
        // Get subtitle element
        // get part name element
        // remove these from maintitlestring
        // do the non-sort stuff and remove further from maintitlestring
        // create nonsort element
        // create main title element
        
        /*
         * Create a NonSortTitleElement if it exists, either as a bf:title
         * object with language "x-bf-sort", or by matching one of the 
         * NON_SORT_STRINGS.
         */
        String sortTitleString = null;
        String nonSortString = null;
        
        // If there's a sort title object of bf:title
        if (bfSortTitleLiteral != null) {
            bfSortTitleLiteral = normalizeTitle(bfSortTitleLiteral);
            sortTitleString = bfSortTitleLiteral.getLexicalForm();
            // Reverse the strings because StringUtils.difference() returns
            // the remainder of the second string, starting from where they
            // differ.
            String reverseSortTitleString = 
                    StringUtils.reverse(sortTitleString);
            LOGGER.debug("Reverse sort title: \"" 
                    + reverseSortTitleString + "\"");
            String reverseMainTitleString = 
                    StringUtils.reverse(mainTitleString);
            LOGGER.debug("reverse main title: \"" 
                    + reverseMainTitleString + "\"");
            LOGGER.debug("Found sort title: \"" + sortTitleString + "\"");
            String difference = 
                    StringUtils.difference(
                            reverseSortTitleString, reverseMainTitleString);
            if (! difference.isEmpty()) {
                nonSortString = StringUtils.reverse(difference);
                LOGGER.debug("Found non sort string: \"" 
                        + nonSortString + "\"");
                mainTitleString = sortTitleString;
                LOGGER.debug("Found main title string: \"" 
                        + mainTitleString + "\"");
    
            }
                  
        } else {
            
            // Look for a match to one of the specified non-sort strings
            for (String string : NON_SORT_STRINGS) {
                if (mainTitleString.startsWith(string)) {
                    nonSortString = string;
                    LOGGER.debug("Found match of main title \"" 
                            + mainTitleString + "' to non-sort string \""
                            + nonSortString + "\"");
                    mainTitleString = StringUtils.difference(
                            nonSortString, mainTitleString);
                    break;                            
                }
            }
        }
 
        // Create the MainTitleElement
        LOGGER.debug("Main title string: \"" + mainTitleString + "\"");
        Resource mainTitleElement = createTitleElement(
                Ld4lType.MAIN_TITLE_ELEMENT, mainTitleString,  
                titleLabelLanguage, localNamespace, false);       
        title = addTitleElement(title, mainTitleElement);
        
        // Create the NonSortTitleElement
        if (nonSortString != null) {
            Resource nonSortElement = 
                    createTitleElement(Ld4lType.NON_SORT_TITLE_ELEMENT, 
                    nonSortString, titleLabelLanguage, localNamespace, false);
            nonSortElement.addProperty(
                    Ld4lProperty.PRECEDES.property(), mainTitleElement);
            title = addTitleElement(title, nonSortElement);           
        }
 
        return titleModel;
    }
    
    private static Resource addTitleElement(
            Resource title, Resource titleElement) {
        
        // Combine the models
        Model titleElementModel = titleElement.getModel();
        Model titleModel = title.getModel();
        titleModel.add(titleElementModel);
        titleElementModel.close();
        
        // Attach the TitleElement to the Title
        title.addProperty(Ld4lProperty.HAS_PART.property(), titleElement);
        
        return title;
    }

    /*
     * This method creates the simplest complete title: there are no title
     * elements other than the required MainTitleElement that all Titles have.
     */
    static Resource createSimpleTitle(
            Literal titleValue, String localNamespace) {

        titleValue = normalizeTitle(titleValue);
        
        Resource title = createTitle(titleValue, localNamespace, false);
            
        // TODO *** Analyze the Title value for a NonSortElement, using the
        // NON_SORT_STRINGS. See convertBfTitleDataProp.

        // Create the MainTitleElement
        Resource mainTitleElement = createTitleElement(
                Ld4lType.MAIN_TITLE_ELEMENT, titleValue, localNamespace, 
                false);
        
        // Attach the MainTitleElement to the Title
        title = addTitleElement(title, mainTitleElement);

        return title;
    }

    /*
     * Creates a TitleElement based on a Bibframe property.
     */
    static Resource createTitleElement(Ld4lType titleElementType, 
            String titleElementString, String titleElementLanguage, 
            String localNamespace, boolean normalize) {
        
        Literal titleElement = ResourceFactory.createLangLiteral(
                titleElementString, titleElementLanguage);
        return createTitleElement(titleElementType, titleElement, 
                localNamespace, normalize);
    }
    
    /*
     * Creates a TitleElement based on a Bibframe property.
     */ 
    static Resource createTitleElement(Ld4lType titleElementType,
            Literal titleElementValue, String localNamespace, 
            boolean normalize) {

        Model model = ModelFactory.createDefaultModel();
        
        // If the literal was not already normalized in a previous step, 
        // normalize it now.
        if (normalize) {
            titleElementValue = normalizeTitle(titleElementValue);
        }
        
        // Create the TitleElement                                                                                                                                                   
        Resource titleElement = 
                model.createResource(RdfProcessor.mintUri(localNamespace));
        model.add(titleElement, RDF.type, titleElementType.type());
        model.add(titleElement, RDFS.label, titleElementValue);

        return titleElement;                  
    }

    
    // Used in two cases: (1) processing the Work/Instance bf:title sort title.
    // (2) processing the Title entity when the Work/Instance has a bf:title
    // sort title.
    private static String getSortTitle(Resource bibResource) {

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
    
    private static Resource createTitle(
            Literal titleValue, String localNamespace, boolean normalize) {

        // Normalize the title string value if caller has passed in an
        // unnormalized string.
        if (normalize) {
            titleValue = normalizeTitle(titleValue);
        }
        
        // Create the Title 
        Model model = ModelFactory.createDefaultModel();
        Resource title = model.createResource(
                RdfProcessor.mintUri(localNamespace));
        title.addProperty(RDF.type, Ld4lType.TITLE.type());
        title.addProperty(RDFS.label, titleValue);
        
        return title;
    }

    // TODO Call from convertBfTitleDataProp()
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
    
    static Literal normalizeTitle(Literal title) {
        
        String language = title.getLanguage();
        String text = title.getLexicalForm();
        String normalizedText = normalizeTitle(text);
        Literal normalizedTitle = 
                ResourceFactory.createLangLiteral(normalizedText, language);
        return normalizedTitle;
    }

    static String normalizeTitle(String title) {
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

}
