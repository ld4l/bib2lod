package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.BfType;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfHeldItemConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfHeldItemConverter.class);
    
    private static final List<BfType> TYPES_TO_CONVERT = 
            new ArrayList<BfType>();
    static {
        TYPES_TO_CONVERT.add(BfType.BF_HELD_ITEM);
    }
 
    private static final List<BfProperty> PROPERTIES_TO_CONVERT = 
            new ArrayList<BfProperty>();
    static {
        PROPERTIES_TO_CONVERT.add(BfProperty.BF_HOLDING_FOR);
    }

    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP =
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_LABEL, Ld4lProperty.NAME);
    }
    
            
    // Bibframe models shelf mark types with different properties; LD4L models
    // them with subclasses of Classification. Map the Bibframe property to the
    // LD4L type.
    private static final Map<BfProperty, Ld4lType> 
            SHELF_MARK_PROP_TO_SUBCLASS = new HashMap<BfProperty, Ld4lType>();            
    static {
        SHELF_MARK_PROP_TO_SUBCLASS.put(BfProperty.BF_SHELF_MARK, 
                Ld4lType.SHELF_MARK);
        SHELF_MARK_PROP_TO_SUBCLASS.put(BfProperty.BF_SHELF_MARK_DDC, 
                Ld4lType.DDC_SHELF_MARK);
        SHELF_MARK_PROP_TO_SUBCLASS.put(BfProperty.BF_SHELF_MARK_LCC, 
                Ld4lType.LCC_SHELF_MARK);
        SHELF_MARK_PROP_TO_SUBCLASS.put(BfProperty.BF_SHELF_MARK_NLM, 
                Ld4lType.NLM_SHELF_MARK);
        SHELF_MARK_PROP_TO_SUBCLASS.put(BfProperty.BF_SHELF_MARK_UDC, 
                Ld4lType.UDC_SHELF_MARK);
    }
    
    private static final Map<BfProperty, Vocabulary> SHELF_MARK_VOCABS =
            new HashMap<BfProperty, Vocabulary>();
    static {
        SHELF_MARK_VOCABS.put(BfProperty.BF_SHELF_MARK_DDC, Vocabulary.DDC);
        SHELF_MARK_VOCABS.put(BfProperty.BF_SHELF_MARK_LCC, Vocabulary.LCC);
        SHELF_MARK_VOCABS.put(BfProperty.BF_SHELF_MARK_NLM, Vocabulary.NLM);
        SHELF_MARK_VOCABS.put(BfProperty.BF_SHELF_MARK_UDC, Vocabulary.UDC);
    }
    
    public BfHeldItemConverter(BfType bfType) {
        super(bfType);
    }
    
    @Override
    protected void convert() {
        convertShelfMark();
        super.convert();
    }
    
    
    private void convertShelfMark() {
        
        Property newProp = Ld4lProperty.HAS_SHELF_MARK.property();

        // Iterate through the Bibframe shelf mark properties
        for (BfProperty bfShelfMarkProp 
                : SHELF_MARK_PROP_TO_SUBCLASS.keySet()) {
           
            Property oldProp = bfShelfMarkProp.property();
            Statement stmt = subject.getProperty(oldProp);

            // If there's a statement using this property
            if (stmt != null ) {
                
                // Get the object of the statement
                Literal shelfMarkLiteral = stmt.getLiteral();

                // If the general bf:shelfMark property is used, look for a
                // bf:shelfMarkScheme statement to define the type of shelfMark.
                if (bfShelfMarkProp.equals(BfProperty.BF_SHELF_MARK)) {
                    // Use the property corresponding to that type of shelf 
                    // mark instead of the generic property.
                    bfShelfMarkProp = 
                            getBfPropertyFromScheme(bfShelfMarkProp);  
                }
                // If no shelf mark scheme was found, we still have the generic
                // shelf mark property. Just keep the literal value. (This 
                // would be an anomalous record, since a shelf mark literal 
                // with no scheme specification makes no sense.
                if (bfShelfMarkProp.equals(BfProperty.BF_SHELF_MARK)) {
                    // Add to assertions model rather than main model, so the
                    // statement doesn't get reprocessed.
                    assertions.add(subject, newProp, shelfMarkLiteral);
                } else {
                    String shelfMarkUri = mintShelfMarkUri(bfShelfMarkProp);                           
                    Resource shelfMark = model.createResource(shelfMarkUri); 
                    // Add to assertions model rather than main model, so the
                    // statements don't get reprocessed.
                    assertions.add(subject, newProp, shelfMark);
                    Ld4lType shelfMarkType = 
                            SHELF_MARK_PROP_TO_SUBCLASS.get(bfShelfMarkProp);
                    assertions.add(shelfMark, RDF.type, 
                            shelfMarkType.ontClass());
                    assertions.add(shelfMark, RDF.value, shelfMarkLiteral);
                }
                
                subject.removeAll(oldProp);
            }
        }        
    }

    /**
     * For a generic bf:shelfMark statement, look for a bf:shelfMarkScheme 
     * assertion from which to determine the type of shelf mark. If it exists 
     * and is one of the identified schemes, return the corresponding 
     * bfProperty; otherwise, return the original bf:shelfMark property.
     */
    private BfProperty getBfPropertyFromScheme(BfProperty shelfMarkProp) {
             
        Property shelfMarkSchemeProp = 
                BfProperty.BF_SHELF_MARK_SCHEME.property();
        Statement schemeStmt = subject.getProperty(shelfMarkSchemeProp);
        if (schemeStmt != null) {
            String schemeValue = schemeStmt.getString();
            for (Map.Entry<BfProperty, Vocabulary> vocabEntry 
                    : SHELF_MARK_VOCABS.entrySet()) {
                // Placeholder code: we don't know what the scheme strings will
                // look like.
                if (StringUtils.containsIgnoreCase(schemeValue, 
                        vocabEntry.getValue().label())) {
                    shelfMarkProp = vocabEntry.getKey();
                    break;
                }                            
            }
            subject.removeAll(shelfMarkSchemeProp);
        }
        return shelfMarkProp;
        
    }
    
    /**
     * Mint a URI for a specific shelf mark using the URI of the vocabulary 
     * plus an arbitrary label constructed from the shelf mark string value.
     * This localname is intended to be temporary! Are the shelf mark values
     * institution-specific or common to all instances of a holding?
     * @param shelfMarkProp
     * @return
     */
    private String mintShelfMarkUri(BfProperty shelfMarkProp) {
        
        String namespace = SHELF_MARK_VOCABS.get(shelfMarkProp).uri();
        return RdfProcessor.mintUri(namespace);
    }

    @Override
    protected List<BfType> getBfTypesToConvert() {
        return TYPES_TO_CONVERT;
    }

    @Override
    protected List<BfProperty> getBfPropertiesToConvert() {
        return PROPERTIES_TO_CONVERT;
    }
   
    @Override
    protected Map<BfProperty, Ld4lProperty> getBfPropertyMap() {
        return PROPERTY_MAP;
    }
}
