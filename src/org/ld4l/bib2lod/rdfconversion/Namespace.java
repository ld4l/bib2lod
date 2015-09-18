package org.ld4l.bib2lod.rdfconversion;

import java.util.HashMap;
import java.util.Map;

public enum Namespace {

    // Assign prefixes here rather than getting from the models, since they may
    // not be provided. We're using these just to create filenames, not for
    // anything significant about the ontologies themselves.
    BIBFRAME("http://bibframe.org/vocab/", "bf"),
    LD4L("http://bibframe.ld4l.org/ontology/", "ld4l"),
    DCTERMS("http://purl.org/dc/terms/", "dcterms"),
    FOAF("http://http://xmlns.com/foaf/0.1/", "foaf"),
    MADSRDF("http://www.loc.gov/mads/rdf/v1#", "madsrdf"), // check if needed
    RDFS("http://http://www.w3.org/2000/01/rdf-schema#", "rdfs"),
    RELATORS("http://id.loc.gov/vocabulary/relators/", "marcrel"); // check if needed
    
    private final String uri;
    private final String prefix;
    
    Namespace(String uri, String prefix) {
        this.uri = uri;
        this.prefix = prefix;
    }
    
    public String uri() {
        return this.uri;
    }
    
    public String prefix() {
        return this.prefix;
    }
    
    private static final Map<String, Namespace> LOOKUP_BY_URI = 
            new HashMap<String, Namespace>();

    private static final Map<String, String> LOOKUP_BY_PREFIX = 
            new HashMap<String, String>();
    
    static {
        for (Namespace ns : Namespace.values()) {
            LOOKUP_BY_URI.put(ns.uri, ns);
            LOOKUP_BY_PREFIX.put(ns.uri, ns.prefix);
        }
    }
    
    public static Namespace get(String uri) { 
        return LOOKUP_BY_URI.get(uri); 
    }
    
    public static String getNsPrefix(String uri) {
        return LOOKUP_BY_PREFIX.get(uri);
    }

}
