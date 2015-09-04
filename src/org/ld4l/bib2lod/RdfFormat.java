package org.ld4l.bib2lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.riot.RDFFormat;

public enum RdfFormat {

    NTRIPLES("ntriples", RDFFormat.NTRIPLES, "nt"),
    RDFXML("rdfxml", RDFFormat.RDFXML, "rdf");

    private final String label;
    private final RDFFormat format;
    private final String extension;
    
    RdfFormat(String label, RDFFormat format, String extension) {
        this.label = label;
        this.format = format;
        this.extension = extension;
    }
    
    public String label() {
        return this.label;
    }
    
    public RDFFormat format() {
        return this.format;
    }
    
    public String extension() {
        return this.extension;
    }

    public String fullExtension() {
        return "." + this.extension;
    }
 
    private static final Map<String, RdfFormat> lookupByLabel =
            new HashMap<String, RdfFormat>();
   
    private static List<RdfFormat> validFormats =
            new ArrayList<RdfFormat>();
   
    static {
        for (RdfFormat rf : RdfFormat.values()) {
            String label = rf.label();
            lookupByLabel.put(label, rf);
            validFormats.add(rf);
        }
    }

    public static RdfFormat get(String label) {
        return lookupByLabel.get(label);
    }
   
    public static List<RdfFormat> validFormats() {
        return validFormats;
    }
}
