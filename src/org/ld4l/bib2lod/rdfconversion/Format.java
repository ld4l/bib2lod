package org.ld4l.bib2lod.rdfconversion;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.riot.RDFFormat;

public enum Format {

    // Currently the only valid output format is ntriples. See notes in 
    // Bib2Lod.getValidFormat().
    NTRIPLES("ntriples", "nt", RDFFormat.NTRIPLES);
    // RDFXML("rdfxml", RDFFormat.RDFXML, "rdf");

    private final String label;
    private final String extension;
    private RDFFormat jenaRDFFormat;
    
    Format(String label, String extension, RDFFormat jenaRDFFormat) {
        this(label, extension);
        this.jenaRDFFormat = jenaRDFFormat;
    }
    
    Format(String label, String extension) {
        this.label = label;
        this.extension = extension;  
        this.jenaRDFFormat = null;
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
 
    private static final Map<String, Format> LOOKUP_BY_LABEL =
            new HashMap<String, Format>();
   
    static {
        for (Format rf : Format.values()) {
            String label = rf.label;
            LOOKUP_BY_LABEL.put(label, rf);
        }
    }

    public static Format get(String label) {
        return LOOKUP_BY_LABEL.get(label);
    }
   
    public static List<Format> allFormats() {
        return new ArrayList<Format>(EnumSet.allOf(Format.class));
    }
}
