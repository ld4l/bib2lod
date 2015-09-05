package org.ld4l.bib2lod;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.riot.RDFFormat;

public enum RdfFormat {

    // Currently the only valid output format is ntriples. See notes in 
    // Bib2Lod.getValidFormat().
    NTRIPLES("ntriples", RDFFormat.NTRIPLES, "nt");
    // RDFXML("rdfxml", RDFFormat.RDFXML, "rdf");

    private final String label;
    private final RDFFormat jenaRDFFormat;
    private final String extension;
    
    RdfFormat(String label, RDFFormat jenaRDFFormat, String extension) {
        this.label = label;
        this.jenaRDFFormat = jenaRDFFormat;
        this.extension = extension;
    }
    
    public String label() {
        return this.label;
    }
    
    public RDFFormat jenaRDFFormat() {
        return this.jenaRDFFormat;
    }
    
    public String extension() {
        return this.extension;
    }

    public String fullExtension() {
        return "." + this.extension;
    }
 
    private static final Map<String, RdfFormat> LOOKUP_BY_LABEL =
            new HashMap<String, RdfFormat>();
   
    static {
        for (RdfFormat rf : RdfFormat.values()) {
            String label = rf.label();
            LOOKUP_BY_LABEL.put(label, rf);
        }
    }

    public static RdfFormat get(String label) {
        return LOOKUP_BY_LABEL.get(label);
    }
   
    public static List<RdfFormat> allFormats() {
        return new ArrayList<RdfFormat>(EnumSet.allOf(RdfFormat.class));
    }
}
