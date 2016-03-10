package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
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

    private static final Map<BfProperty, Ld4lType> PROPERTY_TO_TYPE =
            new HashMap<BfProperty, Ld4lType>();
    static {
        PROPERTY_TO_TYPE.put(BfProperty.BF_ABBREVIATED_TITLE, 
                Ld4lType.ABBREVIATED_TITLE);
        PROPERTY_TO_TYPE.put(BfProperty.BF_KEY_TITLE, Ld4lType.KEY_TITLE);
        // There is also bf:variantTitle, but it's not clear what to do with
        // that, so not assigning a subtype.
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
        
        Model model = subject.getModel();       
        Resource bibResource = getRelatedBibResource();

        StmtIterator stmts = model.listStatements();
        while (stmts.hasNext()) {
            
            Statement statement = stmts.nextStatement();
            Resource stmtSubject = statement.getSubject();
            Property property = statement.getPredicate();
            RDFNode object = statement.getObject();
            BfProperty bfProp = BfProperty.get(property);
            
            if (stmtSubject.equals(bibResource)) {
                addTitleSubType(bibResource, bfProp);
                continue;
            }

//            if (bfProp.equals(BfProperty.BF_TITLE_VALUE) {
//                convertTitleValue();
//                
//            } //else if (bfProp.equals)
        }
        
        return super.convert();
    }
    
    private void addTitleSubType(Resource bibResource, BfProperty bfProp) {

        if (PROPERTY_TO_TYPE.containsKey(bfProp)) {
            Ld4lType titleType = PROPERTY_TO_TYPE.get(bfProp);
            outputModel.add(subject, RDF.type, titleType.type());
            outputModel.add(
                    bibResource, Ld4lProperty.HAS_TITLE.property(), subject);
        }           
        // Type ld4l:Title will be assigned in super.convert();
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
    
//    private void convertTitleValue() {
//        
//        Statement titleValueStmt = 
//                subject.getProperty(BfProperty.BF_TITLE_VALUE.property());
//        
//        if (titleValueStmt != null) {
//
//            // bf:titleValue string and language
//            String titleValueString = 
//                    normalizeTitle(titleValueStmt.getString());
//            String titleValueLanguage = titleValueStmt.getLanguage();
//            
//            // This will be the rdfs:label of the Title
//            String fullTitleString = titleValueString;
//       
//            Resource nonSortElement = createNonSortTitleElement(
//                    titleValueString, titleValueLanguage, subject, outputModel);
//
//            // This will be the rdfs:label value of the MainTitleElement. In
//            // the absence of other title elements, it's the full bf:titleValue
//            // string. If there's a non-sort element, remove it from the full title
//            // title string to get the main title element string.         
////            String mainTitleString = 
////                    removeNonSortString(titleValueString, nonSortElement);
//               
////            Resource mainTitleElement = 
////                    createTitleElement(Ld4lType.MAIN_TITLE_ELEMENT, 
////                            mainTitleString, titleValueLanguage);
//            
//            // Non-sort element precedes main title element
////            if (nonSortElement != null) {
////                outputModel.add(nonSortElement, Ld4lProperty.PRECEDES.property(), 
////                        mainTitleElement);
////            }
//            
//            Resource subtitleElement = null;
//                         
//            Statement subtitleStmt = 
//                    subject.getProperty(BfProperty.BF_SUBTITLE.property());            
//            if (subtitleStmt != null) {
//
//                // Create the subtitle element
//                String subtitleValue = normalizeTitle(subtitleStmt.getString());
//                String language = subtitleStmt.getLanguage();
////                subtitleElement = createTitleElement(
////                        Ld4lType.SUBTITLE_ELEMENT, subtitleValue, language);
//                
//                // When there's a subtitle, the titleValue contains only the 
//                // main title, not the full title. The rdfs:label (full title)
//                // concatenates both main title and subtitle strings.
//                fullTitleString += " " + subtitleValue;
//                
//                // Order the mainTitleElement and subtitleElement
////                outputModel.add(
////                        mainTitleElement, Ld4lProperty.PRECEDES.property(), 
////                        subtitleElement);
//          
//            } 
//            
//            // Add the rdfs:label of the Title
//            outputModel.add(
//                    subject, RDFS.label, fullTitleString, titleValueLanguage);
//            
//            // NB Some Title objects have both a bf:titleValue and a bf:label, 
//            // with different strings. We'll end up assigning two rdfs:labels.
//            // Ideally we should not have two rdfs:labels, but it's not clear
//            // how to resolve this case. In at least some cases the bf:label
//            // has extraneous, non-title text, but it's not clear whether that's
//            // always the case. E.g., record 2083918 has
//            // title5 bf:label "Hans Bethe papers, [ca. 1931]-1992."
//            // title5 bf:titleValue "Hans Bethe papers,"
//            
            // TODO ** Part name/number elements - use bf:partName property
            // and partNumber property
//        }
//    }

}
