package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResIterator;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

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
            + "?bib ?bfTitle ?titleVal . } "
            + "WHERE { { "
            + "?bib ?bibProp ?resource . "
            + "OPTIONAL { "
            + "?bib ?bfTitle ?titleVal . "
            + "FILTER (?bfTitle = " + BfProperty.BF_TITLE.sparqlUri() + ") } "
            + "} UNION { "
            + "?resource ?titleProp ?titlePropObj . "
            + "} }");

    private static final Map<BfProperty, Ld4lType> TITLE_PROP_TO_TYPE =
            new HashMap<BfProperty, Ld4lType>();
    static {
        TITLE_PROP_TO_TYPE.put(BfProperty.BF_ABBREVIATED_TITLE, 
                Ld4lType.ABBREVIATED_TITLE);
        TITLE_PROP_TO_TYPE.put(BfProperty.BF_KEY_TITLE, Ld4lType.KEY_TITLE);
        // TODO Skipping bf:titleVariation for now. Probably a Title-Title
        // relationship.
    }
    
    private static final Map<BfProperty, Ld4lType> 
            TITLE_ELEMENT_PROP_TO_TYPE = new HashMap<BfProperty, Ld4lType>();
    static {
        TITLE_ELEMENT_PROP_TO_TYPE.put(BfProperty.BF_SUBTITLE, 
                Ld4lType.SUBTITLE_ELEMENT);
        TITLE_ELEMENT_PROP_TO_TYPE.put(BfProperty.BF_PART_NUMBER, 
                Ld4lType.PART_NUMBER_TITLE_ELEMENT);
        TITLE_ELEMENT_PROP_TO_TYPE.put(BfProperty.BF_PART_TITLE, 
                Ld4lType.PART_NAME_TITLE_ELEMENT);
    }
    
    public BfTitleConverter(String localNamespace) {
        super(localNamespace);
    }
    
    @Override
    protected ParameterizedSparqlString getResourceSubModelPss() {
        LOGGER.debug(RESOURCE_SUBMODEL_PSS);
        return RESOURCE_SUBMODEL_PSS;
    }

    public Model convert() {
        
        Resource bibResource = getRelatedBibResource();
        
        // Title with no related bib resource (anomalous but found in Cornell 
        // data).
        if (bibResource == null) {
            return outputModel;
        }
        
        addTitleSubType(bibResource);
        addTitleElements(bibResource);
        
        // Do we need to call super.convert(). Add some logging to see what it's
        // doing for title conversion.
        return super.convert();
    }

    /* 
     * Get the Work or Instance related to the Title.
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
    
    private void addTitleSubType(Resource bibResource) {
        
        Model model = subject.getModel();

        StmtIterator stmts = model.listStatements(bibResource, null, subject);
        
        // There will be only one.
        while (stmts.hasNext()) {
            Statement stmt = stmts.nextStatement();
            BfProperty bfProp = BfProperty.get(stmt.getPredicate());
            if (TITLE_PROP_TO_TYPE.containsKey(bfProp)) {
                Ld4lType titleType = TITLE_PROP_TO_TYPE.get(bfProp);
                outputModel.add(subject, RDF.type, titleType.type());
                outputModel.add(bibResource, Ld4lProperty.HAS_TITLE.property(),
                        subject);
            }   
        }
        // Type ld4l:Title will be assigned in super.convert();
        // If we do it here, can we skip super.convert() altogether?
    }
   
    private void addTitleElements(Resource bibResource) {
        
        Literal labelLiteral = getNormalizedLabel();
        
        // Title with no label or titleValue. Not sure if it exists.
        if (labelLiteral == null) {
            return;
        }

        // Create any TitleElements and add them to the model
        // TODO For now we ignore multiple subtitles for a title. Not sure if
        // they show up in the MARC record, anyway.

        String mainTitleLabel = createMainTitleLabel(labelLiteral);
 
        // TODO Following search for a nonSortLabel duplicates the TitleUtils
        // method. Consolidate into a single method. Problem is the method
        // alters both nonSortLabel and mainTitleLabel.
        
        // Look for a nonSortElement, either from a bf:title language 
        // "x-bf-sort", or by matching the NON_SORT_STRINGS.              
        String sortTitleLabel = getSortTitleLabel(bibResource);
        
        Map<String, String> labels = TitleUtils.getNonSortAndMainTitleLabels(
                sortTitleLabel, mainTitleLabel);
                
        // Unpack the labels
        String nonSortLabel = labels.get("nonSortLabel");
        mainTitleLabel = labels.get("mainTitleLabel");       

        Resource mainTitleElement = 
                createTitleElement(Ld4lType.MAIN_TITLE_ELEMENT, mainTitleLabel); 
        
        if (nonSortLabel != null) {
            Resource sortTitleElement = createTitleElement(
                    Ld4lType.NON_SORT_TITLE_ELEMENT, nonSortLabel); 
            // This is the only case where we can reliably add a precedence
            // relationship. There can be multiple subtitles, part
            // numbers, and part name elements, and the Bibframe RDF does not
            // indicate how to order them. One would have to compare to the
            // title label.
            outputModel.add(sortTitleElement, Ld4lProperty.PRECEDES.property(),
                    mainTitleElement);
        }       
         
        createTitleElements(BfProperty.BF_SUBTITLE);
        createTitleElements(BfProperty.BF_PART_NUMBER);        
        createTitleElements(BfProperty.BF_PART_TITLE);

        // addPrecedenceRelations(titleElements);
  
    }

/* 
 * We can't reliably create precedence relations when there are multiple
 * elements of a single type. Example: Cornell bib 2717:
    <http://draft.ld4l.org/cornell/2717title5> <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://bibframe.org/vocab/Title> . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/label> "The history and rudiments of architecture; embracing, I The orders of architecture; II Architectural styles of various countries; III The nature and principles of design in architecture; and IV An accurate and complete glossary of architectural terms ... Ed. by John Bullock." . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/titleValue> "The history and rudiments of architecture;" . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/subtitle> "embracing" . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/partNumber> "I" . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/partNumber> "II" . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/partNumber> "III" . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/partNumber> "IV" . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/partTitle> "The orders of architecture;" . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/partTitle> "Architectural styles of various countries;" . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/partTitle> "The nature and principles of design in architecture; and" . 
        <http://draft.ld4l.org/cornell/2717title5> <http://bibframe.org/vocab/partTitle> "An accurate and complete glossary of architectural terms ..." . 

 * We do not have enough information in the Bibframe to order the part numbers 
 * and names correctly. Is this data in the MARC?
 * 
    private void addPrecedenceRelations(List<Resource> titleElements) {
  
        int last = (titleElements.size()) - 1;
        for (Resource titleElement : titleElements) {
            int index = titleElements.indexOf(titleElement);
            if (index < last) {
                outputModel.add(
                        titleElement, Ld4lProperty.PRECEDES.property(),
                        titleElements.get(index + 1));                        
            }

        }         
    }
 */
    

    private String getSortTitleLabel(Resource bibResource) {
        
        String label = null;

        StmtIterator stmts = 
                bibResource.listProperties(BfProperty.BF_TITLE.property());
        while (stmts.hasNext()) {
            Statement stmt = stmts.nextStatement();
            String language = stmt.getLanguage();
            if (language != null && language.equals("x-bf-sort")) {
                label = stmt.getString();
                break;
            }
        }
        return label;
    }
    
    
    private String getTitlePropLabel(BfProperty titleProp) {
        
        String label = null;
        
        // Find a statement using DatatypeProperty titleProp
        Statement stmt = subject.getProperty(titleProp.property());

        // Get the String object of the statement
        if (stmt != null) {
            label = stmt.getString();
        }
        
        return label;            
    }
    
    // Add the normalized label to the Title
    private Literal getNormalizedLabel() {
  
        Statement stmt = null;
        
        stmt = subject.getProperty(BfProperty.BF_LABEL.property());

        if (stmt == null) {
            stmt = subject.getProperty(BfProperty.BF_TITLE_VALUE.property());
        }
        
        if (stmt == null) {
            return null;
        }
        
        Literal literal = stmt.getLiteral();
 
        // Get new label literal with normalized string
        String label = TitleUtils.normalize(literal.getLexicalForm());
                
        Literal normalizedLiteral = ResourceFactory.createLangLiteral(
                label, literal.getLanguage());
        outputModel.add(
                subject, Ld4lProperty.LABEL.property(), normalizedLiteral);

        return normalizedLiteral;
    }

    
    // TODO - combine with the code in convertBfTitleDataProp() - may just need
    // to parameterize the title and the model. There the title is a local 
    // resource, here it is the subject instance variable. There the model is 
    // the subject's model, but here it is the output model.
    private void createTitleElements(BfProperty titleProp) { 

        // Get the Ld4lType to create
        Ld4lType type = 
                TITLE_ELEMENT_PROP_TO_TYPE.get(titleProp);
        
        StmtIterator stmts = subject.listProperties(titleProp.property());
        while (stmts.hasNext()) {
            Statement stmt = stmts.nextStatement();

            String label = stmt.getString();
  
            createTitleElement(type, label);           
        }        
    }
    
    private Resource createTitleElement(Ld4lType type, String label) {
        
        // Create the TitleElement
        Resource titleElement = TitleUtils.createTitleElement(type, 
                label, null, localNamespace, true);
        
        // Add the TitleElement statements to the output model
        Model model = titleElement.getModel();
        outputModel.add(model);
        model.close();
        
        // Attach the TitleElement to the Title
        outputModel.add(
                subject, Ld4lProperty.HAS_PART.property(), titleElement);
        
        return titleElement;
    }

    
    private String createMainTitleLabel(Literal titleLabelLiteral) {
               
        String mainTitleLabel = null;
        
        // If there's a bf:titleValue, get the MainTitleElement value from it.
        // The titleValue has subtitle, partNumberTitle, and partNameTitle
        // stripped away.
        Statement titleValueStmt = 
                subject.getProperty(BfProperty.BF_TITLE_VALUE.property());
        if (titleValueStmt != null) {
            Literal titleValueLiteral = titleValueStmt.getLiteral();
            titleValueLiteral = TitleUtils.normalize(titleValueLiteral);
            mainTitleLabel = titleValueLiteral.getLexicalForm();
        }
        
        if (mainTitleLabel == null) {
            if (titleLabelLiteral != null) {
                // Start by assigning the Title label to the MainTitleElement
                mainTitleLabel = titleLabelLiteral.getLexicalForm();
                
                // Get the non-normalized string values of the title props. 
                // These will be stripped out of the label to create the 
                // MainTitleElement string value. They should be non-normalized, 
                // else stripping them away may leave stranded punctuation in 
                // the MainTitleElement string.

                mainTitleLabel = removeTitleElementLabel(mainTitleLabel, 
                        BfProperty.BF_SUBTITLE);
                mainTitleLabel = removeTitleElementLabel(mainTitleLabel, 
                        BfProperty.BF_PART_NUMBER);
                mainTitleLabel = removeTitleElementLabel(mainTitleLabel, 
                        BfProperty.BF_PART_TITLE);                
     
                // Reduce sequences of whitespace left by the string removals. 
                mainTitleLabel = 
                        mainTitleLabel.replace("\\s+", " ");
            }
        }
 
        return mainTitleLabel;
    }
    
    private String removeTitleElementLabel(String mainTitleLabel, 
            BfProperty bfTitleProp) {
        
        String titleElementLabel = getTitlePropLabel(bfTitleProp);
                
        if (titleElementLabel != null) {
            mainTitleLabel = mainTitleLabel.replace(titleElementLabel, "");
        }  
        
        return mainTitleLabel;
    }

    
    @Override
    protected Map<Property, Property> getPropertyMap() {
        
        // WRONG - alters map returned by BfProperty.propertyMap()
        // Map<Property, Property> propertyMap = BfProperty.propertyMap();      
        // propertyMap.putAll(getPropertyMap());
        Map<Property, Property> propertyMap = new HashMap<Property, Property>();
        
        // Get default mapping from Bibframe to LD4L properties
        propertyMap.putAll(BfProperty.propertyMap());
        
        // BfTitleConverter normalizes the label and adds an rdfs:label
        // assertion to the outputModel. We don't want super.convert() to
        // convert bf:label to rdfs:label with the original string value.
        propertyMap.remove(BfProperty.BF_LABEL.property());
        
        return propertyMap;
    }

}
