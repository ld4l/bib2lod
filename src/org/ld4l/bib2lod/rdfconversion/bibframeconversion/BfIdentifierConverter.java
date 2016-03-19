package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
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
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

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
    
//    private static final List<BfProperty> BF_IDENTIFIER_PROPS =
//            new ArrayList<BfProperty>();
//    static {
//        BF_IDENTIFIER_PROPS.addAll(PROPERTY_TO_TYPE.keySet());
//        BF_IDENTIFIER_PROPS.add(BfProperty.BF_IDENTIFIER);
//        BF_IDENTIFIER_PROPS.add(BfProperty.BF_SYSTEM_NUMBER);
//    }
//    
//    private static final List<Property> IDENTIFIER_PROPS = 
//            BfProperty.properties(BF_IDENTIFIER_PROPS);
    
    private static final SortedMap<String, Ld4lType> IDENTIFIER_PREFIXES = 

            new TreeMap<String, Ld4lType>(
                    Comparator.comparingInt(String::length)
                    .reversed()            
                    // TreeMap considers two keys that are equal according to 
                    // the  comparator to be equal, so only one is included. 
                    // Must sort by reverse length and then  by natural 
                    // ordering in order to retain strings of the same length. 
                    .thenComparing(String.CASE_INSENSITIVE_ORDER));
            
    static {
        // Not sure what types these prefixes represent. Add when known.
        //        IDENTIFIER_PREFIXES.put("CStRLIN", );
        //        IDENTIFIER_PREFIXES.put("NIC", );        
        IDENTIFIER_PREFIXES.put("(OCoLC)", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("(OCoLC-I)", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("(OCoLC-M)", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("oc", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("ocm", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("ocn", Ld4lType.OCLC_IDENTIFIER);
    }
    
    private Resource relatedResource;
    private Property linkingProperty;
    

    public BfIdentifierConverter(String localNamespace) {
        super(localNamespace);
    }
    
    @Override
    protected Model convert() {

        init();

        if (relatedResource == null) {
            return outputModel;
        }
        
        outputModel.add(relatedResource, Ld4lProperty.IDENTIFIED_BY.property(),
                subject);

        String[] idValues = getIdentifierValue();

        // TODO - move to where it's used
        addIdentifierType(idValues);

        LOGGER.debug("Identifier: " + subject.getURI());
        // RdfProcessor.printModel(outputModel, Level.DEBUG);
        
        return outputModel;
    }
    
    private void init() {

        StmtIterator stmts = 
                subject.getModel().listStatements(null, null, subject);
        while (stmts.hasNext()) {
            Statement stmt = stmts.next();
            relatedResource = stmt.getSubject();
            linkingProperty = stmt.getPredicate();
        }
    }

    private void addIdentifierType(String[] idValues) {
        
        String prefix = idValues[0];
        String value = idValues[1];
        
        Ld4lType identifierType = null;
        
        identifierType = getIdentifierTypeFromPredicate();
        
        // Note that the Bibframe converter may redundantly specify type with 
        // both a predicate and a scheme:
        // <http://draft.ld4l.org/cornell/102063instance16> <http://bibframe.org/vocab/lccn> _:bnode47102063 .
        // _:bnode47102063 <http://bibframe.org/vocab/identifierScheme> <http://id.loc.gov/vocabulary/identifiers/lccn> . 
        if (identifierType == null) {
            identifierType = getIdentifierTypeFromScheme();
        }
        
        if (identifierType == null) {
            identifierType = 
                    getIdentifierTypeFromLiteralValue(prefix, value);                       
        }
  
        // We may want to assign the supertype in any case, to simplify the
        // query used to build the search index, since there's no reasoner to
        // infer it.
        if (identifierType == null) {
            identifierType = Ld4lType.IDENTIFIER;
        }
        
        outputModel.add(subject, RDF.type, identifierType.type());  

    }

    private Ld4lType getIdentifierTypeFromPredicate() {
        
        Ld4lType identifierType = null;
 
        BfProperty bfProp = BfProperty.get(linkingProperty);
                    
        if (bfProp == null) {
            // This would be an oversight; log for review.
            LOGGER.warn("No handling defined for property " 
                    + linkingProperty.getURI() + " linking " 
                    + relatedResource.getURI() + " to its identifier "
                    + subject.getURI() + ". Deleting statement.");   
            
        } else if (PROPERTY_TO_TYPE.keySet().contains(bfProp)) {
            identifierType = PROPERTY_TO_TYPE.get(bfProp);                
        }
           
        return identifierType;        
    }

    private Ld4lType getIdentifierTypeFromScheme() {
        
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
                // Returns null if there's no corresponding type
                identifierType = Ld4lType.getByLocalName(schemeValue);            
            } 
        }
        
        return identifierType;
    }

    private Ld4lType getIdentifierTypeFromLiteralValue(
            String prefix, String idValue) {   
            
        Ld4lType identifierType = null;
 
        // TODO - REDO!
        if (idValue != null) {
            
            if (prefix != null) {               
                identifierType = IDENTIFIER_PREFIXES.get(prefix);
            
            // If no subtype has been identified, and the value is all digits,
            // assume a local ILS identifer.
            // TODO **** Check if this is true for Harvard and Stanford
            } else if (idValue.matches("^\\d+$")
                    // Not sure if this condition is required
                    && linkingProperty.equals(
                            BfProperty.BF_SYSTEM_NUMBER.property())) {

                identifierType = Ld4lType.LOCAL_ILS_IDENTIFIER;
            }
        }
        
        return identifierType;
    }
    
    private String[] getIdentifierValue() {

        // E.g., (OCoLC)234567, ocm234567
        String prefix = null;
        String idValue = null; 
        
        Statement identifierValueStmt = 
                subject.getProperty(BfProperty.BF_IDENTIFIER_VALUE.property());

        if (identifierValueStmt != null) {
            
            RDFNode object = identifierValueStmt.getObject();
            
            // If object is a Resource, pass it through unmodified
            // TODO Check to make sure this happens
            if (object.isLiteral()) {
                
                idValue = object.asLiteral().getLexicalForm();

                // Parse the id value into prefix and value, if possible.
                for (Map.Entry<String, Ld4lType> entry : 
                        IDENTIFIER_PREFIXES.entrySet()) {
                    String key = entry.getKey();
                    if (idValue.startsWith(key)) {
                        prefix = key;
                        idValue = idValue.substring(key.length());                                                                 
                        break; 
                    }                    
                }
                
                outputModel.add(subject, RDF.value, idValue);
            }       
        }
        
        return new String[] { prefix, idValue };       
    }

    protected static Model convertIdentifierLiteral(Resource instance,
            BfProperty identifierProp, Literal value) {
        
        Model model = ModelFactory.createDefaultModel();
                                                                                  
        Ld4lType identifierType = 
                PROPERTY_TO_TYPE.get(identifierProp);
        
        if (identifierType != null) {
            String namespace = instance.getNameSpace();
            Resource identifier = ResourceFactory.createResource(
                    RdfProcessor.mintUri(namespace));
            model.add(instance, Ld4lProperty.IDENTIFIED_BY.property(), 
                    identifier);
            model.add(identifier, RDF.type, identifierType.type());
            model.add(identifier, RDF.value, value);
        }
        
        return model;       
    }
    
    protected static Set<BfProperty> getIdentifierProps() {
        return PROPERTY_TO_TYPE.keySet();
    }
    
}
