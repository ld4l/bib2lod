package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.ParameterizedSparqlString;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.Processor;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
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
        // We don't want to apply this automatically, because if we don't find
        // a specific type from the predicate, we go on to look for it in the
        // identifier scheme and value.
        // PROPERTY_TO_TYPE.put(BfProperty.BF_IDENTIFIER, Ld4lType.IDENTIFIER);
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISAN, Ld4lType.ISAN);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISBN, Ld4lType.ISBN);                     
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISBN10, Ld4lType.ISBN10);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISBN13, Ld4lType.ISBN13);               
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISMN, Ld4lType.ISMN);                
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISO, Ld4lType.ISO);    
        PROPERTY_TO_TYPE.put(BfProperty.BF_ISRC, Ld4lType.ISRC);
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
        //        IDENTIFIER_PREFIXES.put("(CStRLIN)", );
        //        IDENTIFIER_PREFIXES.put("(NIC)", );        
        IDENTIFIER_PREFIXES.put("(OCoLC)", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("(OCoLC-I)", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("(OCoLC-M)", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("oc", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("ocm", Ld4lType.OCLC_IDENTIFIER);
        IDENTIFIER_PREFIXES.put("ocn", Ld4lType.OCLC_IDENTIFIER);
    }

    private static ParameterizedSparqlString RESOURCE_SUBMODEL_PSS = 
            new ParameterizedSparqlString(
                    "CONSTRUCT { ?resource ?p1 ?o . "
                    + "?s ?p2 ?resource . "         
                    + "?s ?local ?id . "
                    + "?id ?ip ?io . "
                    + "} WHERE {  { "                                                      
                    + "?resource ?p1 ?o . "
                    + "} UNION { "
                    + "?s ?p2 ?resource . "
                    + " OPTIONAL { "
                    + "?s ?local ?id . "
                    + "?id ?ip ?io . " 
                    + "FILTER (?local = " + BfProperty.BF_LOCAL.sparqlUri() 
                    + ") "
                    + "FILTER EXISTS { ?s a " 
                    + BfType.BF_INSTANCE.sparqlUri() + " } "
                    + "} "  // end OPTIONAL
                    + "} } ");

    private Resource relatedResource;
    private Property linkingProperty;
    
    public BfIdentifierConverter(String localNamespace) {
        super(localNamespace);
    }
    
    @Override 
    protected ParameterizedSparqlString getResourceSubModelPss() {
        LOGGER.debug(RESOURCE_SUBMODEL_PSS.toString());
        return RESOURCE_SUBMODEL_PSS;
    }
  
    @Override
    protected Model convert() {

        init();

        if (relatedResource == null) {
            return outputModel;
        }

        String[] idValues = parseIdentifierValue();
        String prefix = idValues[0];
        String value = idValues[1];

        if (isDuplicateLocalIdentifier(prefix, value)) {
            return outputModel;
        }
        
        if (value != null) {
            outputModel.add(subject, RDF.value, value);
        } else {
            /* Presumably anomalous. See Cornell 14708:
                <http://draft.ld4l.org/cornell/n14708> <http://bibframe.org/vocab/issnL> _:bnode1748individual14620 . 
                _:bnode1748individual14620 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://bibframe.org/vocab/Identifier> . 
                _:bnode1748individual14620 <http://bibframe.org/vocab/identifierScheme> <http://id.loc.gov/vocabulary/identifiers/issnL> . 
             * versus correct:
                <http://draft.ld4l.org/cornell/n14708> <http://bibframe.org/vocab/issnL> _:bnode1751individual14620 . 
                _:bnode1751individual14620 <http://www.w3.org/1999/02/22-rdf-syntax-ns#type> <http://bibframe.org/vocab/Identifier> . 
                _:bnode1751individual14620 <http://bibframe.org/vocab/identifierScheme> <http://id.loc.gov/vocabulary/identifiers/issnL> . 
                _:bnode1751individual14620 <http://bibframe.org/vocab/identifierValue> "0888-7896" . 
             * Let the identifier be created anyway.
             */
            LOGGER.debug("Found null identifier value for identifier " 
                    + subject.getURI() + " and resource "
                    + relatedResource.getURI());           
        }
        
        addIdentifierType(idValues);

        outputModel.add(relatedResource, Ld4lProperty.IDENTIFIED_BY.property(),
                subject);
        
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

    private String[] parseIdentifierValue() {

        // E.g., values like (OCoLC)234567, ocm234567 are split into a prefix  
        // value. The value is assigned as the rdf:value of the Identifier. The
        // prefix is used to determine the type.
        // There are values with prefixes where the type is currently unknown, 
        // such as (NIC) and (CStRLIN). The entire string is kept as the value,
        // but eventually they should be split into prefix and value.
        String typePrefix = null;
        String value = null; 
        
        Statement identifierValueStmt = 
                subject.getProperty(BfProperty.BF_IDENTIFIER_VALUE.property());

        if (identifierValueStmt != null) {
            
            RDFNode object = identifierValueStmt.getObject();
            
            // Object should always be a literal.
            if (object.isLiteral()) {
                
                value = object.asLiteral().getLexicalForm();

                // Parse the id value into prefix and value, if possible.
                for (Map.Entry<String, Ld4lType> entry : 
                        IDENTIFIER_PREFIXES.entrySet()) {
                    String key = entry.getKey();
                    if (value.startsWith(key)) {
                        typePrefix = key;
                        value = value.substring(key.length());                                                                 
                        break; 
                    }                    
                }
            }       
        }
        
        // TODO - Consider making this a Map, so caller doesn't have to know the
        // order of the strings in the array. (But still has to know the keys, 
        // so maybe no real benefit.) The array is easier to work with in this 
        // context.
        return new String[] { typePrefix, value };       
    }
    
    private boolean isDuplicateLocalIdentifier(
            String thisPrefix, String thisValue) {
 
        // Hack: This is only relevant for Cornell records, which stores the 
        // local identifier in the 035 field which gets picked up by the LC
        // converter. Harvard and Stanford don't have this, so the only trace
        // of the record id is from the original URI generated by the LC 
        // converter from the 001 field.
        String catalog = Processor.getCatalog(localNamespace);
        if (! catalog.equals("cornell")) {
            return false;
        }
        
        if (thisPrefix != null) {
            return false;
        }
        
        Model model = subject.getModel();
        NodeIterator nodes = model.listObjectsOfProperty(
                relatedResource, BfProperty.BF_LOCAL.property());
        
        while (nodes.hasNext()) {
            RDFNode node = nodes.nextNode();
            if (!node.isResource()) {
                return false;
            }

            Resource identifier = node.asResource();
            if (identifier.equals(subject)) {
                return false;
            }
            
            Statement valueStmt = identifier.getProperty(
                BfProperty.BF_IDENTIFIER_VALUE.property());
            if (valueStmt == null) {
                return false;
            }
            
            RDFNode valueObject = valueStmt.getObject();
            
            if (! valueObject.isLiteral()) {
                return false;
            }
            
            String identifierValue = 
                    valueObject.asLiteral().getLexicalForm();
            if (identifierValue.equals(thisValue)) {
                return true;
            }
        }
        
        return false;

    }
    

    
    private void addIdentifierType(String[] idValues) {
        
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
                    getIdentifierTypeFromValue(idValues);                       
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
            
        } else {           
           
           if (PROPERTY_TO_TYPE.keySet().contains(bfProp)) {            
               identifierType = PROPERTY_TO_TYPE.get(bfProp);  
           }
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
    
    private Ld4lType getIdentifierTypeFromValue(String[] idValues) {   
            
        Ld4lType identifierType = null;
        
        String typePrefix = idValues[0];
        String value = idValues[1];
        
        // TODO - Get rid of this statement when we handle LocalIlsIdentifier
        // from UriGenerator
        if (value != null) {
            
            if (typePrefix != null) {               
                identifierType = IDENTIFIER_PREFIXES.get(typePrefix);
            
            // If no subtype has been identified, and the value is all digits,
            // assume a local ILS identifer.
            // TODO **** Check if this is true for Harvard and Stanford
            } else if (value.matches("^\\d+$")
                    // Not sure if this condition is required
                    && linkingProperty.equals(
                            BfProperty.BF_SYSTEM_NUMBER.property())) {

                identifierType = Ld4lType.LOCAL_ILS_IDENTIFIER;
            }
        }
        
        return identifierType;
    }

    protected static Set<BfProperty> getIdentifierProps() {
        return PROPERTY_TO_TYPE.keySet();
    }
    
    public static Model createIdentifier(
            Resource resource, Property resourceProperty,
            Property valueProperty, String value, Resource type) {
        
        Model model = ModelFactory.createDefaultModel();
        
        // Create the new identifier
        Resource identifier = ResourceFactory.createResource(
                RdfProcessor.mintUri(resource.getNameSpace()));
        
        // Link the identifier to the resource
        model.add(resource, resourceProperty, identifier);
   
        // Assign the identifier value
        model.add(identifier, valueProperty,value);
         
        // Assign the identifier type
        model.add(identifier, RDF.type, type);
        
        return model;
    }

}
