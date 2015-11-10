package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

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
import org.apache.jena.rdf.model.Statement;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;

public class BfPersonConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfPersonConverter.class);
    
    private static final Ld4lType NEW_TYPE = Ld4lType.PERSON;
    
    private static final Pattern BF_PERSON_LABEL = 
            //* Which date patterns can occur? Look at more data.
            // dddd-
            // dddd-dddd
            // -dddd
            Pattern.compile("^(.*?)(?:\\s*)(\\d{4})?(?:-)?(\\d{4})?\\.?$");
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_HAS_AUTHORITY, 
                Ld4lProperty.IDENTIFIED_BY_AUTHORITY);
    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            Arrays.asList(
                    BfProperty.BF_AUTHORITY_SOURCE,
                    BfProperty.BF_AUTHORIZED_ACCESS_POINT
            );
    
    public BfPersonConverter(Resource subject) {
        super(subject);        
    }
    

    @Override 
    protected void convertProperties() {
        convertBfLabel();
        super.convertProperties();
    }

    /** 
     * Add properties derived from bf:label to the foaf:Person: name, birthdate,
     * deathdate.
     * @param subject
     * @param model
     * @return
     */
    private void convertBfLabel() {
        
        Property property = BfProperty.BF_LABEL.property();
        Statement stmt = subject.getProperty(property);
        if (stmt != null) {
            Literal literal = stmt.getLiteral();
            String label = literal.getLexicalForm();
            Map<Ld4lProperty, String> labelProps = parseLabel(label);
            for (Map.Entry<Ld4lProperty, String> entry 
                    : labelProps.entrySet()) {
                Ld4lProperty key = entry.getKey();
                String value = entry.getValue();
                if (value != null) {
                    subject.addLiteral(key.property(), value);
                }
            }
        }
        subject.removeAll(property);
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
    

    
    @Override
    protected Ld4lType getNewType() {
        return NEW_TYPE;
    }

    @Override
    protected Map<BfProperty, Ld4lProperty> getPropertyMap() {
        return PROPERTY_MAP;
    }
    
    @Override
    protected List<BfProperty> getPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }
}
