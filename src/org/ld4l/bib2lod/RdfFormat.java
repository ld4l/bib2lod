package org.ld4l.bib2lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.riot.RDFFormat;

public enum RdfFormat {

    // TODO Add other supported types
    NTRIPLES("ntriples", RDFFormat.NTRIPLES, "nt"),
    RDFXML("rdfxml", RDFFormat.RDFXML, "rdf");

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
