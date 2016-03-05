package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
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
        
        Model model = subject.getModel();
        
        // The predicate in these statements should be one of the provider
        // predicates; there are no other statements with the Provider as
        // object.
        StmtIterator statements = model.listStatements(null, null, subject);
        while (statements.hasNext()) {
            Statement statement = statements.next();
            Resource instance = statement.getSubject();
            Property property = statement.getPredicate();
            
            if (PROPERTY_TO_TYPE.containsKey(BfProperty.get(property))) {
                
                outputModel.add(instance, Ld4lProperty.HAS_PROVISION.property(), 
                         subject);                
                BfProperty bfProp = BfProperty.get(property);
                Ld4lType provisionType = PROPERTY_TO_TYPE.containsKey(bfProp) ? 
                        PROPERTY_TO_TYPE.get(bfProp) : Ld4lType.PROVISION;
                outputModel.add(subject, RDF.type, provisionType.ontClass());
                if (provisionType.label() != null) {
                    outputModel.add(subject, RDFS.label, provisionType.label());
                }
            }         
        }
    }

}
