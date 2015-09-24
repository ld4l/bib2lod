package org.ld4l.bib2lod.rdfconversion.typededuping;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntologyType;

// TODO If only abstract methods, change to an interface
public abstract class TypeDeduper {

    private static final Logger LOGGER = 
            LogManager.getLogger(TypeDeduper.class);
    
    private static final Pattern DELETE = Pattern.compile("['\\[\\]\\|]");
    private static final Pattern CONVERT_TO_WHITESPACE = 
            Pattern.compile("[^a-zA-Z0-9#&+,]");
            
    public abstract Map<String, String> dedupe(OntologyType type, Model model);
    
    /**
     * Normalize agent name according to NACO normalization rules:
     * http://www.oclc.org/content/dam/research/publications/library/2006/naco-lrts.pdf
     * http://labs.oclc.org/nacotestbed
     * Use to normalize person and other agent names for identity matching.
     * Currently not handling non-ASCII characters. 
     * TODO Add rules for non-ASCII characters. 
     * TODO Delete diacritics
     * TODO 
     * @param name
     * @return
     */
    protected String normalizeAuthorityName(String name) {
        
        LOGGER.debug("Original name: #" + name + "#");
        
        // Remove specified characters
        Matcher m = DELETE.matcher(name);
        name = m.replaceAll("");
        
        // Convert to lowercase
        name = name.toLowerCase();
        
        // Convert specified characters to space
        m = CONVERT_TO_WHITESPACE.matcher(name);
        name = m.replaceAll(" ");
        
        // Replace all but first comma with whitespace      
        int index = name.indexOf(",");
        if (index > -1) {
            String firstPart = name.substring(0, index+1);
            String secondPart = name.substring(index);
            name = firstPart + secondPart.replace(",", " ");
        }
        
        // Trim whitespace from both ends
        name = StringUtils.strip(name);
        
        // Collapse sequences of whitespace resulting from previous 
        // manipulations; convert to _ so don't need to double-quote as hash
        // keys.
        name = name.replaceAll("\\s+", "_");
        
        LOGGER.debug("Normalized name: " + name);
        return name;
    }
    
    protected String getDefaultKey(QuerySolution soln) {
        // NB It's assumed that bf:authorizedAccessPoint and bf:label values 
        // are identical when both exist. If that turns out to be wrong, we 
        // need some way of resolving discrepancies.

        // Return authorizedAccessPoint value, if there is one
        Literal authAccessPoint = soln.getLiteral("authAccessPoint");
        if (authAccessPoint != null) {
            LOGGER.debug("Using bf:authorizedAccessPoint to dedupe");
            return authAccessPoint.getLexicalForm();
        }
        
        // Else return bf:label, if there is one
        Literal label = soln.getLiteral("label");
        if (label != null) {
            LOGGER.debug("Using bf:label to dedupe");
            return label.getLexicalForm();
        }
        
        return null;
    }

}
