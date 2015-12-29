package org.ld4l.bib2lod.rdfconversion.resourcededuping;

import org.apache.jena.query.QuerySolution;
import org.apache.jena.rdf.model.Literal;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.util.NacoNormalizer;

public class BfPersonDeduper extends BfAuthorityDeduper {

    private static final Logger LOGGER =          
            LogManager.getLogger(BfPersonDeduper.class);
    
    public BfPersonDeduper(BfType type) {
        super(type);
    }
    
    protected String getKey(QuerySolution soln) {
        
        // NB It's assumed that bf:authorizedAccessPoint and bf:label values 
        // are identical when both exist. If that turns out to be wrong, we 
        // need some way of resolving discrepancies.
        
        String key = null;

        // Return authorizedAccessPoint value, if there is one
        Literal authAccessPoint = soln.getLiteral("authAccessPoint");
        if (authAccessPoint != null) {
            LOGGER.debug("Using bf:authorizedAccessPoint to dedupe");
            key = authAccessPoint.getLexicalForm();
            
        } else {       
            // Else return bf:label, if there is one
            Literal label = soln.getLiteral("label");
            if (label != null) {
                LOGGER.debug("Using bf:label to dedupe");
                key = label.getLexicalForm();
            }
        }

        // "Twain, Mark, 1835-1910--Censorship." : dedupe on key
        // "Twain, Mark, 1835-1910". This occurs when the Person is the subject
        // of a Work; we want to reconcile that with the Person from other 
        // contexts, such as author.
        if (key.contains("--")) {
            key = key.substring(0, key.indexOf("--"));
        }
     
        key = NacoNormalizer.normalize(key);
        LOGGER.debug("Normalized key: " + key);
        return key;
        
    }
   
    @Override
    protected String getExternalAuthorityKey(
            QuerySolution soln, String localAuthKey) {
        
        LOGGER.debug("Getting extAuthKey for a bf:Person external auth");
        
        String extAuthKey;
        
        String extAuthLabel = soln.get("extAuthLabel").toString();

        if (extAuthLabel != null) {
            LOGGER.debug("Getting extAuthKey from madsrdf:authoritativeLabel");
            extAuthKey = NacoNormalizer.normalize(extAuthLabel.toString());
            
        // Dedupe on the local authority resource key if there is no 
        // madsrdf:authoritativeLabel. Probably never happens.
        } else {
            LOGGER.debug("No extAuthLabel; using localAuthKey to dedupe");
            extAuthKey = localAuthKey;
        }
        
        LOGGER.debug("extAuthKey: " + extAuthKey);
        return extAuthKey;
    }
}
