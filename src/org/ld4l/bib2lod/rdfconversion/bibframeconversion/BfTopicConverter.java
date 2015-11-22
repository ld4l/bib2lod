package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.List;

import org.apache.jena.rdf.model.Resource;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfTopicConverter extends BfAuthorityConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfTopicConverter.class);

    private static List<BfProperty> PROPS_TO_REMOVE = 
            new ArrayList<BfProperty>();
    static {
        PROPS_TO_REMOVE.add(BfProperty.BF_HAS_AUTHORITY);
        PROPS_TO_REMOVE.add(BfProperty.BF_SYSTEM_NUMBER);
    }

    public BfTopicConverter(BfType bfType, String localNamespace) {
        super(bfType, localNamespace);
    }
    
    protected void convert() {
        convertFastTopic();
        super.convert();
    }

    /** 
     * FAST topics don't need a system number, since the FAST URI replaces it,
     * so we remove it.
     * They are also not authorities, so we remove the MADS Authority from the
     * model. 
     */
    private void convertFastTopic() {
        String namespace = subject.getNameSpace();
        if (namespace.equals(Vocabulary.FAST.uri())) {
            removeResources(PROPS_TO_REMOVE);  
        }
    }

}
