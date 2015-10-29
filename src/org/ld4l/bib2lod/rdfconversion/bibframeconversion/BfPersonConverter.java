package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;

public class BfPersonConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfPersonConverter.class);
    
    private static final Pattern BF_PERSON_LABEL = 
            //* Which date patterns can occur? Look at more data.
            // dddd-
            // dddd-dddd
            // -dddd
            Pattern.compile("^(.*?)(?:\\s*)(\\d{4})?(?:-)?(\\d{4})?\\.?$");
    

    private static final List<OntProperty> PROPERTIES_TO_RETRACT = 
            Arrays.asList(
                    OntProperty.BF_LABEL,
                    OntProperty.BF_HAS_AUTHORITY,
                    OntProperty.BF_AUTHORIZED_ACCESS_POINT
            );
    
    public BfPersonConverter(OntType type) {
        super(type);        
    }
    
    public Model convert(Resource subject) {
        
        Model model = subject.getModel();

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Before converson");
            RdfProcessor.printModel(model, Level.forName("DEV", 450));
        }

        // Remove Bibframe types and add type foaf:Person
        subject.removeAll(RDF.type);
        addType(subject, OntType.PERSON);   
        
        addLabelProperties(subject, model);
        
        addProperty(subject, OntProperty.BF_HAS_AUTHORITY, 
                OntProperty.MADSRDF_IS_IDENTIFIED_BY_AUTHORITY);

        retractProperties(subject);
            
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Model after conversion:");
            RdfProcessor.printModel(model, Level.forName("DEV", 450));
        }
        
        return model;   
    }

    /** 
     * Add properties derived from bf:label to the foaf:Person: name, birthdate,
     * deathdate.
     * @param subject
     * @param model
     * @return
     */
    private void addLabelProperties(Resource subject, Model model) {
        
        // Model model = subject.getModel();
        
        String bfLabel = getBfLabelValue(subject);
        if (bfLabel != null) {
            Map<OntProperty, String> labelProps = parseLabel(bfLabel);
            for (Map.Entry<OntProperty, String> entry 
                    : labelProps.entrySet()) {
                OntProperty key = entry.getKey();
                String value = entry.getValue();
                if (value != null) {
                    subject.addLiteral(createProperty(key, model), value);
                }
            }
        }
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
    

    protected List<OntProperty> getPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }
}
