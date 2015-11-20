package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.rdf.model.StmtIterator;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;

public class BfInstanceConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfInstanceConverter.class);
    

    private static final Map<Resource, Resource> TYPES_TO_CONVERT = 
            BfType.typeMap(Arrays.asList(
                BfType.BF_ELECTRONIC,
                BfType.BF_INSTANCE,
                BfType.BF_PRINT,
                BfType.BF_TACTILE            
            ));
            
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

    
    private static final List<Resource> TYPES_TO_RETRACT = 
            new ArrayList<Resource>();
    static {
            TYPES_TO_RETRACT.add(BfType.BF_ARCHIVAL.ontClass());
    }
   
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {

    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {

    }
    
    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            new ArrayList<BfProperty>();
    static {
        
    }

    public BfInstanceConverter(BfType bfType) {
        super(bfType);
    }
    
    @Override 
    protected void convert() {
        convertInstanceTypes();
        
        super.convert();
    }
   
    protected void convertInstanceTypes() {
        
        // Get the associated Work
        Statement instanceOf = 
                subject.getProperty(BfProperty.BF_INSTANCE_OF.property());
        Resource relatedWork = instanceOf.getResource();
        
        // Get the Instance's declared types
        StmtIterator stmts = model.listStatements();               
        while (stmts.hasNext()) {
            Statement statement = stmts.nextStatement();
            Property predicate = statement.getPredicate();
            if (predicate.equals(RDF.type)) {
                if (convertInstanceType(statement.getResource(), relatedWork)) {
                    stmts.remove();
                }
                

            }

            

        }
    }
    
    private boolean convertInstanceType(Resource type, Resource relatedWork) {
        // NB This could also be done in super.convert(), but more straight-
        // forward to handle all RDF.type statements in one place. 
        if (TYPES_TO_CONVERT.containsKey(type)) {
            assertions.add(subject, RDF.type, TYPES_TO_CONVERT.get(type));
        } else if (NEW_WORK_TYPES.containsKey(type)) {
            assertions.add(relatedWork, RDF.type, NEW_WORK_TYPES.get(type));
        // Problematic, since we don't have the related items here. And if we did, how
        // would we know which item(s) it applied to?
        // } else if (NEW_ITEM_TYPES.containsKey(type)) {
            
        } else if (TYPES_TO_RETRACT.contains(type)) {
            
        } else {
            return false;
        }
        return true;
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
