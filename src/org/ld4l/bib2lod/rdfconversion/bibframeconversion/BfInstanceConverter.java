package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.BibframeConverter;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class BfInstanceConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfInstanceConverter.class);
    
 
    private static final Map<Resource, Resource> NEW_WORK_TYPES = 
            BfType.typeMap(Arrays.asList(
                    BfType.BF_COLLECTION,
                    BfType.BF_INTEGRATING,
                    BfType.BF_MONOGRAPH,
                    BfType.BF_MULTIPART_MONOGRAPH,
                    BfType.BF_SERIAL
            ));
                        
    /*
     * This case is problematic, since, unlike the Work, we do not have links to
     * the Instance's Items here. Furthermore, how would we know which of the
     * Instance's Items are Manuscripts?
     */
    private static final Map<Resource, Resource> NEW_ITEM_TYPES =
            BfType.typeMap(Arrays.asList(
                    BfType.BF_MANUSCRIPT
            ));
   
    private static final List<BfType> TYPES_TO_RETRACT = 
            new ArrayList<BfType>();
    static {
            TYPES_TO_RETRACT.add(BfType.BF_ARCHIVAL);
    }
   
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {

    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }

    private static final List<BfProperty> PROVIDER_PROPERTIES = 
            new ArrayList<BfProperty>();
    static {
        PROVIDER_PROPERTIES.add(BfProperty.BF_DISTRIBUTION);
        PROVIDER_PROPERTIES.add(BfProperty.BF_MANUFACTURE);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PRODUCTION);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PROVIDER);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PUBLICATION);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        // These get removed in BfProviderConverter
        // PROPERTIES_TO_RETRACT.addAll(PROVIDER_PROPERTIES);
    }

    public BfInstanceConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    @Override 
    protected void convertModel() {
        
        convertProviders();
      
        StmtIterator stmts = model.listStatements();
        while (stmts.hasNext()) {
            
            Statement statement = stmts.nextStatement();
            Property predicate = statement.getPredicate();
            
            if (predicate.equals(RDF.type)) {
                if (convertInstanceTypeToWorkType(statement.getResource())) {
                    stmts.remove();
                }
                
                // Handle conversion to Item type here, when we know how...
                
            // TODO Do we ever get a providerStatement statement instead of a
            // Provider object? If so, we need to parse the literal value to
            // create the Provision and its associated properties.
            // } else if (predicate.equals(BfProperty.BF_PUBLICATION.property())) {
                // convertProvider(statement);
                //stmts.remove();
            
            } //else convertIdentifier()
              // else convertTitle()
        }
        
        super.convertModel();
    }
   

    private boolean convertInstanceTypeToWorkType(Resource type) {

        if (NEW_WORK_TYPES.containsKey(type)) {
            
            // Get the associated Work
            Statement instanceOf = 
                    subject.getProperty(BfProperty.BF_INSTANCE_OF.property());
            Resource relatedWork = instanceOf.getResource();
            assertions.add(relatedWork, RDF.type, NEW_WORK_TYPES.get(type));
            return true;
        }
 
        return false;
    }
    
    private void convertProviders() {
        
        List<Property> providerProps = 
                BfProperty.propertyList(PROVIDER_PROPERTIES);
        
        List<Statement> providerStmts = new ArrayList<Statement>();
        
        for (Property prop : providerProps) {
            StmtIterator stmts = 
                    model.listStatements(subject, prop, (RDFNode) null);
            stmts.forEachRemaining(providerStmts::add);
            
            // NB This version generates a ConcurrentModificationException when
            // we delete stmt from the model in BfProviderConverter. The 
            // version with the for-loop does not, because, though we are 
            // altering the model, we are not altering the object we're looping
            // through.
            while (stmts.hasNext()) {
                Statement stmt = stmts.nextStatement();
                convertProvider(stmt);
            }
        }
        
        for (Statement stmt : providerStmts) {
            convertProvider(stmt);
        }                   
    }
    
    private void convertProvider(Statement statement) {
        
        // Type the converter as BfProviderConverter rather than 
        // BfResourceConverter, else we must add a vacuous 
        // convertSubject(Resource, Statement) method to BfResourceConverter.
        BfProviderConverter converter = new BfProviderConverter(
              BfType.BF_PROVIDER, this.localNamespace);
        
        // Identify the provider resource and build its associated model (i.e.,
        // statements in which it is the subject or object).
        Resource provider = BibframeConverter.getSubjectModelToConvert(
                statement.getResource());
                
        assertions.add(converter.convertSubject(provider, statement));
    }
    
    @Override
    protected List<BfType> getBfTypesToRetract() {
        return TYPES_TO_RETRACT;
    }
    

    @Override
    protected List<BfProperty> getBfPropertiesToConvert() {
        return PROPERTIES_TO_CONVERT;
    }
   
    @Override
    protected Map<BfProperty, Ld4lProperty> getBfPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected List<BfProperty> getBfPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }

}
