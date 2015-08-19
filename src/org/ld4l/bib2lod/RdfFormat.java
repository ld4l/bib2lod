package org.ld4l.bib2lod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum RdfFormat {

    // TODO add other supported types
    RDFXML ("rdfxml", "rdf"),
    NTRIPLES ("ntriples", "nt");

    private final String format;
    private final String extension;

    RdfFormat(String format, String extension) {
        this.format = format;
        this.extension = extension;
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
 
    private static final Map<String, RdfFormat> lookup = 
            new HashMap<String, RdfFormat>();
    
    private static List<String> validFormats = 
            new ArrayList<String>();
    
    static {
        for(RdfFormat rf : RdfFormat.values()) {
            String format = rf.format();
            lookup.put(format, rf);
            validFormats.add(format);
        }
    }

    public static RdfFormat get(String format) { 
        return lookup.get(format); 
    }
    
    public static List<String> validFormats() {
        return validFormats;
    }
    
}
