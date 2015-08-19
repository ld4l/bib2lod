package org.ld4l.bib2lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.jena.riot.RDFFormat;

// TODO - just discovered Jena RDFFormat. Can we use it or extend it
// instead of defining our own enum?

public enum RdfFormat {

    // TODO add other supported types
    RDFXML ("rdfxml", "rdf", RDFFormat.RDFXML),
    NTRIPLES ("ntriples", "nt", RDFFormat.NTRIPLES);

    private final String format;
    private final String extension;
    private final RDFFormat rdfFormat;

    RdfFormat(String format, String extension, RDFFormat rdfFormat) {
        this.format = format;
        this.extension = extension;
        this.rdfFormat = rdfFormat;
    }
    
    public String format() {
        return this.format;
    }
    
    public String extension() {
        return this.extension;
    }
    
    public String fullExtension() {
        return "." + this.extension;
    }
    
    public RDFFormat rdfFormat() {
        return this.rdfFormat;
    }
 
    private static final Map<String, RdfFormat> lookup = 
            new HashMap<String, RdfFormat>();
    
    private static final Map<RDFFormat, RdfFormat> lookupByRDFFormat = 
            new HashMap<RDFFormat, RdfFormat>();
    
    private static List<String> validFormats = 
            new ArrayList<String>();
    
    static {
        for(RdfFormat rf : RdfFormat.values()) {
            String format = rf.format();
            lookup.put(format, rf);
            RDFFormat rdfFormat = rf.rdfFormat;
            lookupByRDFFormat.put(rdfFormat, rf);
            validFormats.add(format);
        }
    }

    public static RdfFormat get(String format) { 
        return lookup.get(format); 
    }
    
    public static RdfFormat getByRDFFormat(RDFFormat format) {
        return lookupByRDFFormat.get(format);
    }
    
    public static List<String> validFormats() {
        return validFormats;
    }
    
}
