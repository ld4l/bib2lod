package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.RdfProcessor;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfHeldItemConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfHeldItemConverter.class);

            
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
    
    public BfHeldItemConverter(String localNamespace) {
        super(localNamespace);
    }
    
    @Override
    protected Model convert() {
        convertShelfMark();
        return super.convert();
    }
    
    
    private void convertShelfMark() {
        
        Property ld4lProp = Ld4lProperty.HAS_SHELF_MARK.property();

        // Iterate through the Bibframe shelf mark properties
        for (BfProperty bfShelfMarkProp 
                : SHELF_MARK_PROP_TO_SUBCLASS.keySet()) {
           
            Property bfProp = bfShelfMarkProp.property();
            Statement stmt = subject.getProperty(bfProp);

            // If there's a statement using this property
            if (stmt != null) {
                
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
                    // Add to outputModel model rather than main model, so the
                    // statement doesn't get reprocessed.
                    outputModel.add(subject, ld4lProp, shelfMarkLiteral);
                } else {
                    String shelfMarkUri = mintShelfMarkUri(bfShelfMarkProp);                           
                    Resource shelfMark = 
                            outputModel.createResource(shelfMarkUri); 
                    // Add to outputModel model rather than main model, so the
                    // statements don't get reprocessed.
                    outputModel.add(subject, ld4lProp, shelfMark);
                    Ld4lType shelfMarkType = 
                            SHELF_MARK_PROP_TO_SUBCLASS.get(bfShelfMarkProp);
                    outputModel.add(shelfMark, RDF.type, 
                            shelfMarkType.ontClass());
                    outputModel.add(shelfMark, RDF.value, shelfMarkLiteral);
                }
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
    
    private String mintShelfMarkUri(BfProperty shelfMarkProp) {
        
        // These namespaces don't create resolvable URIs; they are just a 
        // stand-in for possible future vocabularies.
        // Are shelf marks for a resource the same across libraries, or are 
        // they catalog-specific?
        String namespace = SHELF_MARK_VOCABS.get(shelfMarkProp).uri();
        return RdfProcessor.mintUri(namespace);
    }

}
