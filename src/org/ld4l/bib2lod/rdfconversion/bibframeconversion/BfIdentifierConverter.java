package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.OWL;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfIdentifierConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfIdentifierConverter.class);
    
    private static final Map<BfProperty, Ld4lType> PROPERTY_TO_TYPE = 
            new HashMap<BfProperty, Ld4lType>();
    static {
        PROPERTY_TO_TYPE.put(BfProperty.BF_ANSI, Ld4lType.ANSI);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_CODEN, Ld4lType.CODEN);             
        PROPERTY_TO_TYPE.put(BfProperty.BF_DISSERTATION_IDENTIFIER,                
                Ld4lType.DISSERTATION_IDENTIFIER);
        PROPERTY_TO_TYPE.put(BfProperty.BF_DOI, Ld4lType.DOI);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_EAN, Ld4lType.EAN);              
        PROPERTY_TO_TYPE.put(BfProperty.BF_FINGERPRINT, Ld4lType.FINGERPRINT);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_HDL, Ld4lType.HDL); 
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISAN, Ld4lType.ISAN);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISBN, Ld4lType.ISBN);                     
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISBN10, Ld4lType.ISBN10);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISBN13, Ld4lType.ISBN13);               
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISMN, Ld4lType.ISMN);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISO, Ld4lType.ISO);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISSN, Ld4lType.ISSN);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISSNL, Ld4lType.ISSNL);               
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISSUE_NUMBER, Ld4lType.ISSUE_NUMBER);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISTC, Ld4lType.ISTC);              
        PROPERTY_TO_TYPE.put( BfProperty.BF_ISWC, Ld4lType.ISWC);                   
        PROPERTY_TO_TYPE.put(BfProperty.BF_LC_OVERSEAS_ACQ, 
                Ld4lType.LC_OVERSEAS_ACQ_NUMBER);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_LCCN, Ld4lType.LCCN);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_LEGAL_DEPOSIT, 
                Ld4lType.LEGAL_DEPOSIT_NUMBER);               
        PROPERTY_TO_TYPE.put(BfProperty.BF_LOCAL, 
                Ld4lType.LOCAL_ILS_IDENTIFIER);               
        PROPERTY_TO_TYPE.put(BfProperty.BF_MATRIX_NUMBER, 
                Ld4lType.MATRIX_NUMBER);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_MUSIC_PLATE, 
                Ld4lType.MUSIC_PLATE_NUMBER);               
        PROPERTY_TO_TYPE.put(BfProperty.BF_MUSIC_PUBLISHER_NUMBER, 
                Ld4lType.MUSIC_PUBLISHER_NUMBER);
        PROPERTY_TO_TYPE.put(BfProperty.BF_NBAN, Ld4lType.NBAN);
        PROPERTY_TO_TYPE.put(BfProperty.BF_NBN, Ld4lType.NBN);
        PROPERTY_TO_TYPE.put(BfProperty.BF_POSTAL_REGISTRATION, 
                Ld4lType.POSTAL_REGISTRATION_NUMBER);
        PROPERTY_TO_TYPE.put(BfProperty.BF_PUBLISHER_NUMBER, 
                Ld4lType.PUBLISHER_NUMBER);
        PROPERTY_TO_TYPE.put(BfProperty.BF_REPORT_NUMBER, 
                Ld4lType.TECHNICAL_REPORT_NUMBER);
        PROPERTY_TO_TYPE.put(BfProperty.BF_SICI, Ld4lType.SICI);
        PROPERTY_TO_TYPE.put(BfProperty.BF_STOCK_NUMBER, Ld4lType.STOCK_NUMBER);
        PROPERTY_TO_TYPE.put(BfProperty.BF_STRN, Ld4lType.STRN);
        PROPERTY_TO_TYPE.put(BfProperty.BF_STUDY_NUMBER, Ld4lType.STUDY_NUMBER);
        PROPERTY_TO_TYPE.put(BfProperty.BF_UPC, Ld4lType.UPC);
        PROPERTY_TO_TYPE.put(BfProperty.BF_VIDEORECORDING_NUMBER, 
                Ld4lType.VIDEO_RECORDING_NUMBER);
    }
    
    private static final List<BfProperty> BF_IDENTIFIER_PROPS =
            new ArrayList<BfProperty>();
    static {
        BF_IDENTIFIER_PROPS.addAll(PROPERTY_TO_TYPE.keySet());
        BF_IDENTIFIER_PROPS.add(BfProperty.BF_IDENTIFIER);
        BF_IDENTIFIER_PROPS.add(BfProperty.BF_SYSTEM_NUMBER);
    }
    
    private static final List<Property> IDENTIFIER_PROPS = 
            BfProperty.properties(BF_IDENTIFIER_PROPS);
    
    private static final SortedMap<String, Ld4lType> IDENTIFIER_PREFIXES = 
// TreeMap considers two keys that are equal according to the comparator to be
// equal, so one is deleted from the map. Must sort by length and then by
// 
//          new TreeMap<String, Ld4lType>(
//           Comparator.comparingInt(String::length));      
            new TreeMap<String, Ld4lType>(
                    Comparator.comparingInt(String::length)
                    .thenComparing(String.CASE_INSENSITIVE_ORDER));

            
    static {
        // Many of these are not instantiated in the data, so it's a guess as
        // to what the string value would be.
//        IDENTIFIER_PREFIXES.put("CStRLIN", );
//        IDENTIFIER_PREFIXES.put("NIC", );        
        IDENTIFIER_PREFIXES.put("(OCoLC)", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("(OCoLC-I)", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("(OCoLC-M)", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("oc", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("ocm", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("ocn", Ld4lType.OCLC_IDENTIFIER);
    }
    

    public BfIdentifierConverter(String localNamespace, Statement statement) {
        super(localNamespace, statement);
    }

    public static List<Property> getIdentifierProps() {
        return IDENTIFIER_PROPS;
    }
    
    protected Model convertModel() {
        
//        LOGGER.debug("Identifier model: ");
//        RdfProcessor.printModel(inputModel, Level.DEBUG);

        Resource relatedResource = linkingStatement.getSubject();

        if (isWorldcatUri(subject)) {
            convertWorldcatIdentifier(relatedResource);
            return outputModel;
        }
            
        outputModel.add(relatedResource, Ld4lProperty.IDENTIFIED_BY.property(),
                subject);

        Statement identifierValueStmt = 
                subject.getProperty(BfProperty.BF_IDENTIFIER_VALUE.property());

        RDFNode identifierValue = null;
        String prefix = null;
        String id = null;
        String newIdentifierValue = null;
        if (identifierValueStmt != null) {
            identifierValue = identifierValueStmt.getObject();
            if (identifierValue.isLiteral()) {
                newIdentifierValue = 
                        identifierValue.asLiteral().getLexicalForm();
                
                for (Map.Entry<String, Ld4lType> entry : 
                        IDENTIFIER_PREFIXES.entrySet()) {
                    String key = entry.getKey();
                    if (newIdentifierValue.startsWith(key)) {
                        prefix = key;
                        id = newIdentifierValue.substring(key.length());
                        LOGGER.debug(prefix + " + " + id);                       
                    }
                    
                }
            
            // TODO Are there URIs for identifierValue, other than worldcat?
                
            // This is true of worldcat, but is it otherwise true?
            // } else {
            //   newIdentifierValue = 
            //           identifierValue.asResource().getLocalName();
            }
        }
        
        
        /*
         * Identifier type
         */
        
        Ld4lType identifierType = 
                getIdentifierTypeFromPredicate(relatedResource);
        
        // Note that Biframe often redundantly specifies type with both a 
        // predicate and a scheme:
        // <http://draft.ld4l.org/cornell/102063instance16> <http://bibframe.org/vocab/lccn> _:bnode47102063 .
        // _:bnode47102063 <http://bibframe.org/vocab/identifierScheme> <http://id.loc.gov/vocabulary/identifiers/lccn> . 
        if (identifierType == null) {
            identifierType = getIdentifierTypeFromIdentifierScheme();
        }
        
        if (identifierType == null) {
            identifierType = getIdentifierTypeFromIdentifierValue(prefix, 
                        newIdentifierValue);
        }
  
        // We may want to assign the supertype in any case, to simplify the
        // queries used to build the search index, since there's no reasoner to
        // infer the supertype.
        if (identifierType == null) {
            identifierType = Ld4lType.IDENTIFIER;
        }
        
        outputModel.add(subject, RDF.type, identifierType.ontClass());  
        
        
        /*
         * Identifier value                                
         */

        if (newIdentifierValue != null) {
            // If we've assigned a non-generic Identifier (i.e., a subclass of
            // Identifier rather than the generic superclass), then remove any
            // prefix from the value, since that is a type indicator rather 
            // than part of the value.
            if (prefix != null && 
                    ! identifierType.equals(Ld4lType.IDENTIFIER)) {
                newIdentifierValue = id;
                
            }
            outputModel.add(subject, RDF.value, newIdentifierValue);
            
            // From string value "(OCoLC)449860683" create 
            // :instance owl:sameAs <http://www.worldcat.org/oclc/449860683>
            // instances based on the identifier values.
            Resource worldcatInstance = outputModel.createResource(
                    Vocabulary.WORLDCAT.uri() + newIdentifierValue);
            if (identifierType.equals(Ld4lType.OCLC_IDENTIFIER)) {
                outputModel.add(relatedResource, OWL.sameAs, worldcatInstance);
            }
            
        }

        return outputModel;
    }
   

    private void convertWorldcatIdentifier(Resource relatedResource) {
        
        // Add an identifier object with the Worldcat id as its value.
        Resource identifier = outputModel.createResource(
                RdfProcessor.mintUri(localNamespace));
        outputModel.add(relatedResource, Ld4lProperty.IDENTIFIED_BY.property(), 
                identifier);
        outputModel.add(identifier, RDF.type, 
                Ld4lType.OCLC_IDENTIFIER.ontClass());
        
        // Jena doesn't recognize localname starting with digit.
        // outputModel.add(identifier, RDF.value, subject.getLocalName());
        String id = subject.getURI().replaceAll(Vocabulary.WORLDCAT.uri(), "");
        outputModel.add(identifier, RDF.value, id);
        
        // Also add an owl:sameAs assertion from the instance to the Worldcat
        // URI.
        outputModel.add(relatedResource, OWL.sameAs, subject);
    }

    private Ld4lType getIdentifierTypeFromIdentifierValue(
            String prefix, String newIdentifierValue) {   
            
        Ld4lType identifierType = null;
        
        if (newIdentifierValue != null) {
            
            if (prefix != null) {               
                identifierType = IDENTIFIER_PREFIXES.get(prefix);
                
            } else if (newIdentifierValue.matches("^\\d+$")
                    // Not sure if this condition is required
                    && linkingStatement.getPredicate().equals(
                            BfProperty.BF_SYSTEM_NUMBER.property())) {

                identifierType = Ld4lType.LOCAL_ILS_IDENTIFIER;
            }

        }
        
        return identifierType;
    }

    private Ld4lType getIdentifierTypeFromIdentifierScheme() {
        
        Ld4lType identifierType = null;
        String schemeValue = null;
        
        Statement schemeStmt = subject.getProperty(
                BfProperty.BF_IDENTIFIER_SCHEME.property());
        if (schemeStmt != null) {
            RDFNode scheme = schemeStmt.getObject();
            
            // Cornell, Harvard
            if (scheme.isResource()) {
                schemeValue = scheme.asResource().getLocalName();
                
            // Stanford
            } else if (scheme.isLiteral()) {
                schemeValue = scheme.asLiteral().getLexicalForm();
            }
            
            if (! schemeValue.equals("systemNumber")) {
                schemeValue = StringUtils.capitalize(schemeValue);
                // Will be null if it doesn't exist.
                identifierType = Ld4lType.get(schemeValue);            
            } 
        }
        
        return identifierType;
    }

    private Ld4lType getIdentifierTypeFromPredicate(Resource relatedResource) {
            
        Ld4lType identifierType = null;
            
        Property predicate = linkingStatement.getPredicate();
        
        BfProperty bfProp = BfProperty.get(predicate);
                    
        if (bfProp == null) {
            // This would be an oversight; log for review.
            LOGGER.warn("No handling defined for property " 
                    + predicate.getURI() + " linking " 
                    + relatedResource.getURI() + " to its identifier "
                    + subject.getURI() + ".");   
            
        } else if (PROPERTY_TO_TYPE.keySet().contains(bfProp)) {
            identifierType = PROPERTY_TO_TYPE.get(bfProp);                
        }
           
        return identifierType;        
    }
    
    private boolean isWorldcatUri(Resource resource) {
        // Jena doesn't recognize the namespace when the localname starts with
        // a digit.
        // if (resource.getNameSpace().equals(Vocabulary.WORLDCAT.uri())) {
        return resource.getURI().startsWith(Vocabulary.WORLDCAT.uri());
    }
    

}
