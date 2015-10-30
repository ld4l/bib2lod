package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Arrays;
import java.util.List;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.OntProperty;
import org.ld4l.bib2lod.rdfconversion.OntType;

public class BfFamilyConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfFamilyConverter.class);
    
    private static final List<OntProperty> PROPERTIES_TO_RETRACT = 
            Arrays.asList(
                    OntProperty.BF_LABEL,
                    OntProperty.BF_HAS_AUTHORITY,
                    OntProperty.BF_AUTHORIZED_ACCESS_POINT
            );
            
    
    public BfFamilyConverter(Resource subject) {
        super(subject);
    }
    
    @Override
    public Model convert() {
        
        Model model = subject.getModel();
        
        subject.removeAll(RDF.type);
        addType(OntType.FAMILY);
        
        addProperty(OntProperty.BF_LABEL, OntProperty.NAME);
        
        addProperty(OntProperty.BF_HAS_AUTHORITY, 
                OntProperty.MADSRDF_IS_IDENTIFIED_BY_AUTHORITY);

        retractProperties();
        return model;
    }



    @Override
    protected List<OntProperty> getPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }

}
