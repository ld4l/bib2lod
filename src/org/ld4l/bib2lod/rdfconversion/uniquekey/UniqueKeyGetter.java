package org.ld4l.bib2lod.rdfconversion.uniquekey;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.resourcededuping.BfResourceDeduper;

public class UniqueKeyGetter {

    private static final Logger LOGGER = 
            LogManager.getLogger(UniqueKeyGetter.class);

    // Using LinkedHashMap in case order of processing becomes important
    private static final Map<Resource, BfType> TYPES_FOR_BF_ONT_CLASSES =
            BfType.typesForOntClasses();
    
    private final Resource resource;
    
    public UniqueKeyGetter(Resource resource) {
        this.resource = resource;
    }

    /* Get the identifying key based on statements in which the resource is 
     * either the subject or object.
     */
    public String getUniqueKey() {
        
        String key = null;
        
        ResourceKeyGetter keyGetter = getKeyGetterByType();
        if (keyGetter != null) {
            key = keyGetter.getKey();
        }
        
        return key;
    }
    
    private ResourceKeyGetter getKeyGetterByType() {

        // Get the BfTypes for this resource
        List<Statement> typeStmts = resource.listProperties(RDF.type).toList();
        List<BfType> types = new ArrayList<BfType>();
        for (Statement stmt : typeStmts) {
            Resource ontClass = stmt.getResource();
            types.add(TYPES_FOR_BF_ONT_CLASSES.get(ontClass));
        }
        
        Class<?> keyGetterClass = null;

        if (types.contains(BfType.BF_TOPIC)) {
            keyGetterClass = BfTopicKeyGetter.class;
        } else if (BfType.isAuthority(types)) {  
            keyGetterClass = BfAuthorityKeyGetter.class;
        } else if (types.contains(BfType.BF_EVENT)) {
            keyGetterClass = BfEventKeyGetter.class;
        } else if (types.contains(BfType.BF_HELD_ITEM)) {
            keyGetterClass = BfHeldItemKeyGetter.class;
        } else if (types.contains(BfType.BF_INSTANCE)) {
            keyGetterClass = BfInstanceKeyGetter.class;
        } else if (types.contains(BfType.BF_WORK)) {
            keyGetterClass = BfWorkKeyGetter.class;
        } else if (types.contains(BfType.BF_IDENTIFIER)) {
            keyGetterClass = BfIdentifierKeyGetter.class;
        } else if (types.contains(BfType.BF_ANNOTATION)) {
            keyGetterClass = BfAnnotationKeyGetter.class;
        } else if (types.contains(BfType.MADSRDF_AUTHORITY)) {
            keyGetterClass = MadAuthorityKeyGetter.class;
        } else if (types.contains(BfType.BF_CLASSIFICATION)) {
            keyGetterClass = BfClassificationKeyGetter.class;
        } else if (types.contains(BfType.BF_LANGUAGE)) {
            keyGetterClass = BfLanguageKeyGetter.class;
        } else if (types.contains(BfType.BF_TITLE)) {
            keyGetterClass = BfTitleKeyGetter.class;          
        } else if (types.contains(BfType.BF_CATEGORY)) {
            keyGetterClass = BfCategoryKeyGetter.class; 
        } else if (types.contains(BfType.BF_PROVIDER)) {
            keyGetterClass = BfProviderKeyGetter.class;
        } else {
            keyGetterClass = ResourceKeyGetter.class;
        }
        
        ResourceKeyGetter keyGetter = null;
        if (keyGetterClass != null) {
            try {
                keyGetter = (ResourceKeyGetter) keyGetterClass
                        .getConstructor(Resource.class).newInstance(resource);                        
            } catch (Exception e) {
                LOGGER.trace("No deduper created for type " + keyGetterClass);
                e.printStackTrace();
            } 
        }
               
        return keyGetter; 
    }
}
