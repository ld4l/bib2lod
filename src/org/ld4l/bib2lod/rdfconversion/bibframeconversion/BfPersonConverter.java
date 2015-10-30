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
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfPersonConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfPersonConverter.class);
    
    private static final OntType NEW_TYPE = OntType.PERSON;
    
    private static final Pattern BF_PERSON_LABEL = 
            //* Which date patterns can occur? Look at more data.
            // dddd-
            // dddd-dddd
            // -dddd
            Pattern.compile("^(.*?)(?:\\s*)(\\d{4})?(?:-)?(\\d{4})?\\.?$");
    
    private static final Map<OntProperty, OntProperty> PROPERTY_MAP =
            new HashMap<OntProperty, OntProperty>();
    static {
        PROPERTY_MAP.put(OntProperty.BF_HAS_AUTHORITY, 
                OntProperty.MADSRDF_IS_IDENTIFIED_BY_AUTHORITY);
    }
    
    private static final List<OntProperty> PROPERTIES_TO_RETRACT = 
            Arrays.asList(
                    OntProperty.BF_AUTHORIZED_ACCESS_POINT
            );
    
    public BfPersonConverter(Resource subject) {
        super(subject);        
    }
    
    @Override
    public Model convert() {       
        assignType();         
        convertBfLabel();
        convertProperties();
        retractProperties();                   
        return subject.getModel();
    }

    /** 
     * Add properties derived from bf:label to the foaf:Person: name, birthdate,
     * deathdate.
     * @param subject
     * @param model
     * @return
     */
    private void convertBfLabel() {
        
        Model model = subject.getModel();
        Property property = createProperty(OntProperty.BF_LABEL, model);
        Statement stmt = subject.getProperty(property);
        if (stmt != null) {
            Literal literal = stmt.getLiteral();
            String label = literal.getLexicalForm();
            Map<OntProperty, String> labelProps = parseLabel(label);
            for (Map.Entry<OntProperty, String> entry 
                    : labelProps.entrySet()) {
                OntProperty key = entry.getKey();
                String value = entry.getValue();
                if (value != null) {
                    subject.addLiteral(createProperty(key, model), value);
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
    Map<OntProperty, String> parseLabel(String label) {

        Map<OntProperty, String> props = new HashMap<OntProperty, String>();
        
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
        
        props.put(OntProperty.NAME, name);
        props.put(OntProperty.BIRTHDATE, birthyear);
        props.put(OntProperty.DEATHDATE, deathyear);
        
        return props;   
    }
    

    
    @Override
    protected OntType getNewType() {
        return NEW_TYPE;
    }

    @Override
    protected Map<OntProperty, OntProperty> getPropertyMap() {
        return PROPERTY_MAP;
    }
    
    @Override
    protected List<OntProperty> getPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }
}
