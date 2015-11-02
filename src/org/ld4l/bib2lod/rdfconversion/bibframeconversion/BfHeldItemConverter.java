package org.ld4l.bib2lod.rdfconversion.bibframeconversion;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.rdf.model.Literal;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.rdf.model.Statement;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.RDFS;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ld4l.bib2lod.rdfconversion.BfProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lProperty;
import org.ld4l.bib2lod.rdfconversion.Ld4lType;
import org.ld4l.bib2lod.rdfconversion.Vocabulary;

public class BfHeldItemConverter extends BfResourceConverter {

    private static final Logger LOGGER = 
            LogManager.getLogger(BfHeldItemConverter.class);
    
    private static final Ld4lType NEW_TYPE = Ld4lType.ITEM;
    
    private static final Map<BfProperty, Ld4lProperty> PROPERTY_MAP = 
            new HashMap<BfProperty, Ld4lProperty>();
    static {
        PROPERTY_MAP.put(BfProperty.BF_HOLDING_FOR, 
                Ld4lProperty.IS_HOLDING_FOR);
        PROPERTY_MAP.put(BfProperty.BF_BARCODE, Ld4lProperty.BARCODE);
    }

    private static final List<BfProperty> PROPERTIES_TO_RETRACT = 
            Arrays.asList(
                    BfProperty.BF_LABEL
            );
            
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
    
    
    public BfHeldItemConverter(Resource subject) {
        super(subject);
    }
    
    @Override 
    public Model convert() {

        assignType(); 
        convertShelfMark();
        convertProperties();
        retractProperties();
        return subject.getModel();
    }
    
    @Override
    protected Ld4lType getNewType() {
        return NEW_TYPE;
    }

    @Override
    protected Map<BfProperty, Ld4lProperty> getPropertyMap() {
        return PROPERTY_MAP;
    }

    @Override
    protected List<BfProperty> getPropertiesToRetract() {
        return PROPERTIES_TO_RETRACT;
    }
    
    private void convertShelfMark() {
        
        Model model = subject.getModel();
        
        // Figure out which shelf mark prop is being used
        // Get the string from the shelf mark prop
        // Mint a url for the shelf mark - what namespace? what localname?
        // Assertions:
        // :item hasShelfMark/hasLocator/locatedAt :shelfMark
        // :shelfMark a ClassificationLcc / etc
        // :shelfMark rdfs:label "original string value"
        
        // If bf:shelfMark is used, structure is more complex: need
        // to get type from shelfMarkScheme

        for (Map.Entry<BfProperty, Ld4lType> entry 
                : SHELF_MARK_PROP_TO_SUBCLASS.entrySet()) {
            BfProperty bfProp = entry.getKey();
            Property property = bfProp.property();
            Statement stmt = subject.getProperty(property);

            if (stmt != null ) {
                Literal literal = stmt.getLiteral();

                // If the general bf:shelfMark property is used, look for a
                // bf:shelfMarkScheme statement to define the type of shelfMark.
                if (bfProp.equals(BfProperty.BF_SHELF_MARK)) {
                    bfProp = getBfPropertyFromScheme(bfProp, model);                           
                    property = bfProp.property();
                }
                String shelfMarkUri = mintShelfMarkUri(bfProp, literal);
                Resource shelfMark = model.createResource(shelfMarkUri);
                subject.addProperty(property, shelfMark);                
                Ld4lType ld4lType = entry.getValue();
                model.add(shelfMark, RDF.type, ld4lType.resource());
                // Not sure if this should be an rdfs:label or some other 
                // property.
                shelfMark.addLiteral(RDFS.label, literal);                
                subject.removeAll(property);
            }
        }        
    }

    /**
     * For bf:shelfMark statement, look for a bf:shelfMarkScheme assertion from
     * which to determine the type of shelf mark. If it exists and is one of
     * the identified schemes, return the corresponding bfProperty; otherwise,
     * return the original bf:shelfMark property.
     * @param statement
     * @param bfProp
     * @param literal
     * @param model
     * @return
     */
    private BfProperty getBfPropertyFromScheme(BfProperty bfProp, Model model) {
    
        Statement schemeStmt = subject.getProperty(
                BfProperty.BF_SHELF_MARK_SCHEME.property());
        if (schemeStmt != null) {
            Literal schemeLiteral = schemeStmt.getLiteral();
            String schemeValue = schemeLiteral.getLexicalForm();
            for (Map.Entry<BfProperty, Vocabulary> vocabEntry 
                    : SHELF_MARK_VOCABS.entrySet()) {
                    if (schemeValue.contains(vocabEntry.getValue().label())) {
                        bfProp = vocabEntry.getKey();
                        break;
                    }                            
            }
        }           
        return bfProp;
        
    }
    
    /**
     * Mint a URI for a specific shelf mark using the URI of the vocabulary 
     * plus an arbitrary label constructed from the shelf mark string value.
     * This localname is intended to be temporary! Are the shelf mark values
     * institution-specific or common to all instances of a holding?
     * @param prop
     * @return
     */
    private String mintShelfMarkUri(BfProperty prop, Literal shelfMark) {       
        String namespace = SHELF_MARK_VOCABS.get(prop).uri();
        String localname = shelfMark.getLexicalForm().replace(" ", "_");
        LOGGER.debug("Minted new shelf mark URI: " + namespace + localname);
        return namespace + localname;
    }
    
}
