package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class IdentifierUtils {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfInstanceConverter.class);

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
    
    private IdentifierUtils() { }

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
    
    protected static Map<BfProperty, Ld4lType> getPropertyToTypeMap() {
        return PROPERTY_TO_TYPE;
    }
    
    protected static Set<BfProperty> getIdentifierProps() {
        return PROPERTY_TO_TYPE.keySet();
    }
}
