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

        String[] idValues = addIdentifierValue();

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
    
    
//    @Override
//    protected ParameterizedSparqlString getResourceSubModelPss() {
//        // Probably can use BfResourceConverter query
//    }

    private String[] addIdentifierValue() {

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
                
                outputModel.add(subject, RDF.value, value);
            }       
        }
        
        return new String[] { typePrefix, value };       
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
            
           Map<BfProperty, Ld4lType> propertyToType = 
                   IdentifierUtils.getPropertyToTypeMap();
           
           if (propertyToType.keySet().contains(bfProp)) {
            
               identifierType = propertyToType.get(bfProp);  
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


    


}
