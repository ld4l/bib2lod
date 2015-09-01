package org.ld4l.bib2lod;

import java.util.HashMap;
import java.util.Map;

public enum Namespace {

    // Assign prefixes here rather than getting from the models, since they may
    // not be provided. We're using these just to create filenames, not for
    // anything significant about the ontologies themselves.
    BIBFRAME ("http://bibframe.org/vocab/", "bf"),
    LD4L ("http://bibframe.ld4l.org/ontology/", "ld4l"),
    DCTERMS ("http://purl.org/dc/terms/", "dcterms"),
    FOAF ("http://http://xmlns.com/foaf/0.1/", "foaf"),
    MADSRDF ("http://www.loc.gov/mads/rdf/v1#", "madsrdf"), // check if needed
    RELATORS ("http://id.loc.gov/vocabulary/relators/", "marcrel"); // check if needed
    
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
    
    private static final Map<String, Namespace> uriToNs = 
            new HashMap<String, Namespace>();

    private static final Map<String, String> uriToPrefix = 
            new HashMap<String, String>();
    
    static {
        for (Namespace ns : Namespace.values()) {
            uriToNs.put(ns.uri(), ns);
            uriToPrefix.put(ns.uri, ns.prefix());
        }
    }
    
    public static Namespace get(String uri) { 
        return uriToNs.get(uri); 
    }
    
    public static String getNsPrefix(String uri) {
        return uriToPrefix.get(uri);
    }

}
