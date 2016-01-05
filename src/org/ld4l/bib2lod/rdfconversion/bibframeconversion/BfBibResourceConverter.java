package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.ResourceFactory;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;

/*
 * Provides conversions shared by Works and Instances - e.g., Title conversion
 * methods.
 */
public abstract class BfBibResourceConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfBibResourceConverter.class);

    private static final List<BfProperty> TITLE_DATATYPE_PROPS = 
            new ArrayList<BfProperty>();  
    static {
        TITLE_DATATYPE_PROPS.add(BfProperty.BF_TITLE);
        TITLE_DATATYPE_PROPS.add(BfProperty.BF_TITLE_STATEMENT);
    }
    
    public BfBibResourceConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
  
    // titleProp is either workTitle or instanceTitle
    protected void convertTitles(BfProperty titleProp) {

        // Get the value of any bf:titleStatement, bf:title, and bf:label 
        // statements. These are datatype properties that need to be converted 
        // to object property statements with a Title as the object.
        List<Statement> titleDatatypeStmts = new ArrayList<Statement>();
        for (BfProperty bfProp : TITLE_DATATYPE_PROPS) {
            List<Statement> stmts = 
                    subject.listProperties(bfProp.property()).toList();
            if (! stmts.isEmpty()) {
                titleDatatypeStmts.addAll(stmts);
            }
        }
        
        retractions.add(titleDatatypeStmts);
        
        // Convert list to set to eliminate duplicates. Can't use a set above
        // because a set can't be added to a model.
        Set<Statement> uniqueTitleDatatypeStmts = 
                new HashSet<Statement>(titleDatatypeStmts);
 
        Set<Literal> titleLiterals = new HashSet<Literal>();
        for (Statement stmt : uniqueTitleDatatypeStmts) {
            Literal literal = stmt.getLiteral();
            // Normalize the title string for comparison to existing Title
            // labels.
            String value = BfTitleConverter.normalizeTitle(literal.getString());
            LOGGER.debug("Adding literal with value '" + value + "' to list");
            titleLiterals.add(ResourceFactory.createLangLiteral(
                    value, literal.getLanguage()));
        }

        List<Statement> titles = inputModel.listStatements(subject, 
                titleProp.property(), (RDFNode) null).toList();
        for (Statement stmt : titles) {
            convertTitle(stmt, titleLiterals);
        }
         
        // Create a new title object for any titleStatement literals that 
        // haven't matched an existing title and therefore been removed from
        // the titleLiterals list.
        // NB We don't have to test each of these literals against one another,
        // because if they're identical Jena will already have removed the 
        // duplicates. If they're not identical (e.g., due to a language value),
        // they should both be retained.
        for (Literal literal : titleLiterals) {
            LOGGER.debug("Creating new title with label " + literal.toString());
            createTitle(literal);            
        }
        
        applyRetractions(inputModel, retractions);           
    }

    private void convertTitle(Statement statement, 
            Set<Literal> titleLiterals) {

        BfResourceConverter converter = 
                new BfTitleConverter(this.localNamespace);
        
        // Identify the title resource and build its associated model (i.e.,
        // statements in which it is the subject or object).
        Resource title = getSubjectModelToConvert(statement.getResource());
                
        Model titleModel = converter.convert(title);
        outputModel.add(titleModel);
        
        // title.getModel() is the original model. Need to get the title 
        // resource from the new model.
        Resource newTitle = titleModel.getResource(title.getURI());
        if (newTitle != null) {
            Statement labelStmt = newTitle.getProperty(RDFS.label); 
            if (labelStmt != null) {

                Literal label = labelStmt.getLiteral(); 
    
                if (label != null) {
                    boolean removed = 
                            titleLiterals.removeIf(i -> label.sameValueAs(i));
                    LOGGER.debug(removed 
                            ? "Found a match for bf:titleStatement '" 
                            + label.toString() + "': removing"
                            : "No bf:titleStatement matching title '" 
                            + label.toString() + "'");
                }
            }
        }       
        titleModel.close();
    }
    
    private void createTitle(Literal label) {
        
        BfTitleConverter converter = new BfTitleConverter(this.localNamespace);
        Model create = converter.create(subject, label);
		outputModel.add(create);
		create.close();
    }
    

}
