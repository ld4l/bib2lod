package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
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

public class BfPersonConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfPersonConverter.class);
    
    private static final Pattern BF_PERSON_LABEL = 
            //* Which date patterns can occur? Look at more data.
            // dddd-
            // dddd-dddd
            // -dddd
            Pattern.compile("^(.*?)(?:\\s*)(\\d{4})?(?:-)?(\\d{4})?\\.?$");

    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_RETRACT.add(BfProperty.BF_LABEL);
    }
    
    public BfPersonConverter(String localNamespace) {
        super(localNamespace);
    }
    
    @Override 
    protected Model convert() {
        
        // Must do before the iteration on model statements, since it requires
        // modification of the label as well.
        convertPersonSubject();
        
        StmtIterator statements = subject.getModel().listStatements();     
        
        while (statements.hasNext()) {
            Statement statement = statements.nextStatement();
            Property predicate = statement.getPredicate();           
            if (predicate.equals(BfProperty.BF_LABEL.property())) {
                convertBfLabel(statement);           
            } 
        }
        
        return super.convert();
    }

    /** 
     * Add properties derived from bf:label to the foaf:Person: name, birthdate,
     * deathdate.
     * @param subject
     * @param model
     * @return
     */
    private void convertBfLabel(Statement statement) {

        Literal labelLiteral = statement.getLiteral();       
        String label = labelLiteral.getLexicalForm();
        String language = labelLiteral.getLanguage();
        parseLabel(label, language);
        //retractions.add(statement);
    }
    
    private void parseLabel(String label, String language) {
        
        Map<Ld4lProperty, String> labelProps = parseLabel(label);
        for (Map.Entry<Ld4lProperty, String> entry 
                : labelProps.entrySet()) {
            Ld4lProperty key = entry.getKey();
            String value = entry.getValue();
            if (value != null) {
                outputModel.add(subject, key.property(), 
                        ResourceFactory.createLangLiteral(value, language));
            }
        }        
    }
    
    private void convertPersonSubject() {
        
        StmtIterator statements = subject.getModel().listStatements(
                null, BfProperty.BF_SUBJECT.property(), subject);
       
       while (statements.hasNext()) {
           
            Resource work = statements.nextStatement().getSubject();
            StmtIterator labelStmts = 
                    subject.listProperties(BfProperty.BF_LABEL.property());
            while (labelStmts.hasNext()) {
                convertPersonSubjectLabel(labelStmts.nextStatement(), work);
            }
            
        }
        //applyRetractions();
    }
    
    private void convertPersonSubjectLabel(
            Statement labelStatement, Resource work) {                                                
            
        Literal labelLiteral = labelStatement.getLiteral();
        String label = labelLiteral.getLexicalForm();
        String language = labelLiteral.getLanguage(); 
        
        // "Hannes Sigfússon--Biography."
        if (label.endsWith(".")) {
            label = label.substring(0, label.length()-1);
        }
        
        // "Twain, Mark, 1835-1910--Censorship."        
        List<String> subjects = Arrays.asList(label.split("--"));
        
        for (String subject : subjects) {
            // The first item contains the personal data
            if (subjects.indexOf(subject) == 0) {        
                parseLabel(subject, language);
               
            } else {
                // Create a new Topic that is also the subject of the related
                // Work.
                // In some cases this should perhaps be a genre/form; e.g., 
                // "Biography", but at this point we have no basis for the
                // distinction.
                Resource newSubject = ResourceFactory.
                        createResource(RdfProcessor.mintUri(localNamespace));
                Literal literal = ResourceFactory.createLangLiteral(
                        subject, language);
                outputModel.add(work, Ld4lProperty.HAS_SUBJECT.property(),
                        newSubject);
                outputModel.add(
                        newSubject, RDF.type, Ld4lType.TOPIC.type());
                outputModel.add(newSubject, 
                        Ld4lProperty.PREFERRED_LABEL.property(), literal);
            }
            
        }
               
        // TODO handle fast uri - need to call Identifier converter
                
        //retractions.add(labelStatement);
    }
    
    /**
     * Parse bf:Person label into name, birth year, death year. No attempt is
     * made to parse the name itself, reverse last and first names, etc.
     * @param label
     * @return
     */
    static Map<Ld4lProperty, String> parseLabel(String label) {

        Map<Ld4lProperty, String> props = new HashMap<Ld4lProperty, String>();
        
        String name = null;
        String birthyear = null;
        String deathyear = null;
        
        Matcher m = BF_PERSON_LABEL.matcher(label);
        if (m.find()) {
            name = m.group(1);
            name = name.replaceAll(",$", "");
            birthyear = m.group(2);
            deathyear = m.group(3);
            LOGGER.debug(name + " | " + birthyear + " | " + deathyear);
        }             
        
        props.put(Ld4lProperty.NAME, name);
        props.put(Ld4lProperty.BIRTHDATE, birthyear);
        props.put(Ld4lProperty.DEATHDATE, deathyear);
        
        return props;   
    }
    
    protected Map<Property, Property> getPropertyMap() {
        
        Map<Property, Property> map = super.getPropertyMap();
        
        // These properties are removed rather than converted.
        map.keySet().removeAll(BfProperty.properties(PROPERTIES_TO_RETRACT));
        
        return map;
    }
}
