package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.NodeIterator;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.BibframeConverter;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

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

    private static final List<BfProperty> PROVIDER_PROPERTIES = 
            new ArrayList<BfProperty>();
    static {
        PROVIDER_PROPERTIES.add(BfProperty.BF_DISTRIBUTION);
        PROVIDER_PROPERTIES.add(BfProperty.BF_MANUFACTURE);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PRODUCTION);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PROVIDER);
        PROVIDER_PROPERTIES.add(BfProperty.BF_PUBLICATION);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_PROVIDER_STATEMENT);
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_SUPPLEMENTARY_CONTENT_NOTE);

    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }

    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_DERIVED_FROM);
    }
    
    public BfInstanceConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    @Override 
    protected void convertModel() {
        
        // Get the associated Work
        Statement instanceOf = 
                subject.getProperty(BfProperty.BF_INSTANCE_OF.property());
        // Probably can't be null
        Resource relatedWork = 
                instanceOf != null ? instanceOf.getResource() : null;
        
        convertTitles(BfProperty.BF_INSTANCE_TITLE);

        List<Statement> statements = new ArrayList<Statement>();        
        StmtIterator stmts = model.listStatements();
        
        // Copy statements to a list and loop through the list rather than
        // using the iterator. This allows us to modify the model inside the
        // loop, since the list itself is not being modified, whereas using an
        // iterator does not allow us to remove the current statement from the
        // model outside the iterator.
        stmts.forEachRemaining(statements::add);
        
        for (Statement statement : statements) {
            
            Property predicate = statement.getPredicate();
    
            if (predicate.equals(RDF.type)) {
                if (convertInstanceTypeToWorkType(
                        statement.getResource(), relatedWork)) {
                    retractions.add(statement);
                }
                
                // Handle conversion to Item type here, when we know how...
              
            } else {
                
                BfProperty bfProp = BfProperty.get(predicate);
                
                if (bfProp == null) {
                    // Log for review, to make sure nothing has escaped.
                    LOGGER.info("No specific handling defined for property " 
                            + predicate.getURI()
                            + "; falling through to default case.");
                    continue;
                }
                
                // TODO Do we ever get a providerStatement instead of a 
                // Provider object? If so, we need to parse the literal value 
                // to create the Provision and its associated properties (cf.
                // titleStatement).
                if (PROVIDER_PROPERTIES.contains(bfProp)) {
                    convertProvider(statement);
                    
                } else if (bfProp.equals(BfProperty.BF_IDENTIFIER) || 
                        bfProp.equals(BfProperty.BF_SYSTEM_NUMBER)) {
                    convertIdentifier(statement);
                                              
                } else if (bfProp.equals(BfProperty.BF_MODE_OF_ISSUANCE)) {
                    if (relatedWork != null) {
                        // Probably also added from conversion of statement
                        // :instance a Monograph
                        assertions.add(relatedWork, RDF.type, 
                                Ld4lType.MONOGRAPH.ontClass());
                        retractions.add(statement);
                    }                   
                }
            }          
        }
        
        applyRetractions();
        
        super.convertModel();
    }
 
    // TODO This method is also used by BfWorkConverter. Need to combine.
    // titleProp = bf:instanceTitle or bf:workTitle
    private void convertTitles(BfProperty titleProp) {

        // Get the value of any bf:titleStatement statements
        StmtIterator titleStmtIterator = subject.listProperties(
                BfProperty.BF_TITLE_STATEMENT.property());

        // This empties out the iterator
        // retractions.add(titleStmtIterator);
        List<Literal> titleLiterals = new ArrayList<Literal>();
        while (titleStmtIterator.hasNext()) {
            Statement stmt = titleStmtIterator.nextStatement();
            Literal literal = stmt.getLiteral();
            // Normalize the title string for comparison to existing Title
            // labels.
            String value = BfTitleConverter.normalizeTitle(literal.getString());
            LOGGER.debug("Adding literal with value '" + value + "' to list");
            titleLiterals.add(
                    model.createLiteral(value, literal.getLanguage()));
            retractions.add(stmt);
        }
        
        StmtIterator titleStmts = model.listStatements(subject, 
                titleProp.property(), (RDFNode) null);

        List<Statement> titles = new ArrayList<Statement>();
        titleStmts.forEachRemaining(titles::add);
        for (Statement stmt : titles) {
            convertTitle(stmt, titleLiterals);
        }
         
        // Create a new title object for any titleStatement literals that 
        // haven't matched an existing title.
        for (Literal literal : titleLiterals) {
            LOGGER.debug("Creating new title with label " + literal.toString());
            createTitle(literal);            
        }
        
        applyRetractions();           
    }

    // TODO This method is also used by BfWorkConverter. Need to combine.
    private void convertTitle(Statement statement, 
            List<Literal> titleLiterals) {

        BfResourceConverter converter = new BfTitleConverter(
              BfType.BF_TITLE, this.localNamespace);
        
        // Identify the title resource and build its associated model (i.e.,
        // statements in which it is the subject or object).
        Resource title = BibframeConverter.getSubjectModelToConvert(
                statement.getResource());

        assertions.add(converter.convertSubject(title));
        
        Literal label = title.getProperty(RDFS.label).getLiteral();
        if (label != null) {
            boolean removed = titleLiterals.removeIf(i -> label.sameValueAs(i));
            LOGGER.debug(removed 
                    ? "Found a match for bf:titleStatement '" 
                    + label.toString() + "': removing"
                    : "Didn't find a match for bf:titleStatement '" 
                    + label.toString() + "'");
        }
    }
    
    private void createTitle(Literal label) {
        
        BfTitleConverter converter = new BfTitleConverter(
                BfType.BF_TITLE, this.localNamespace);
        assertions.add(converter.create(subject, label));
    }
    
    
    private boolean convertInstanceTypeToWorkType(
            Resource type, Resource relatedWork) {

        if (NEW_WORK_TYPES.containsKey(type)) {
            if (relatedWork != null) {
                assertions.add(relatedWork, RDF.type, NEW_WORK_TYPES.get(type));
            }
            return true;
        }
 
        return false;
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
        
        // Add BfProviderConverter model to this converter's assertions model,
        // so they get added to the BibframeConverter output model.
        assertions.add(converter.convertSubject(provider, statement));
    }
    
    private void convertIdentifier(Statement statement) {
        
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
