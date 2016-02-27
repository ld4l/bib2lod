package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfProviderConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfProviderConverter.class);

    private static final Map<BfProperty, Ld4lType> PROPERTY_TO_TYPE = 
            new HashMap<BfProperty, Ld4lType>();
    static {
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_DISTRIBUTION, Ld4lType.DISTRIBUTOR_PROVISION);
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_MANUFACTURE, Ld4lType.MANUFACTURER_PROVISION);
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_PRODUCTION, Ld4lType.PRODUCER_PROVISION);
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_PROVIDER, Ld4lType.PROVISION);
        PROPERTY_TO_TYPE.put(
                BfProperty.BF_PUBLICATION, Ld4lType.PUBLISHER_PROVISION);
    }
    

    public BfProviderConverter(String localNamespace) {
        super(localNamespace);
    }   
    
    @Override
    protected Model convert() {
        
      createProvision();
      
      return super.convert();
    }

    private void createProvision() {
        
//        Property providerProp = linkingStatement.getPredicate();
//        LOGGER.debug(providerProp.getURI());
//        BfProperty bfProp = BfProperty.get(providerProp);
//        Resource instance = linkingStatement.getSubject();
//        
//        Ld4lType newType = PROPERTY_TO_TYPE.containsKey(bfProp) ? 
//                PROPERTY_TO_TYPE.get(bfProp) : Ld4lType.PROVISION;
//   
//        /*
//         * TODO bf:providerRole
//         * If bf:providerRole is specified, this should be replaced with a
//         * specific Provision subtype. If it doesn't match any of the existing
//         * subtypes, convert to a label on the generic ld4l:Provision. Need to
//         * analyze what values, if any, are generated.
//         *
//         * TODO bf:providerStatement
//         * For now will move to legacy namespace. If the statement exists 
//         * without the provider and its object properties, should parse the 
//         * providerStatement to construct these. If it occurs alongside the 
//         * provider and object properties, and the data is the same, it should 
//         * be discarded.
//        */
//
//        // Remove type statement that gets converted to Provision superclass
//        // in super.convertModel()
//// Don't need - doing now by removing mapping in BfType
////        StmtIterator stmts = inputModel.listStatements(
////                subject, RDF.type, BfType.BF_PROVIDER.ontClass());
////        retractions.add(stmts);
//        
//        outputModel.add(subject, RDF.type, newType.ontClass());
//        
//        if (newType.label() != null) {
//            outputModel.add(subject, RDFS.label, newType.label());
//        }
//
//        // Could also let these fall through to super.convertModel(), but might
//        // as well do it now.
//        outputModel.add(
//                instance, Ld4lProperty.HAS_PROVISION.property(), subject);
//        retractions.add(linkingStatement);

    }

}
