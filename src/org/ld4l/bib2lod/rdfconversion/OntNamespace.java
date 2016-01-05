package org.ld4l.bib2lod.rdfconversion;

import java.util.HashMap;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public enum OntNamespace {

    // Assign prefixes here rather than getting from the models, since they may
    // not be provided. We're using these just to create filenames, not for
    // anything significant about the ontologies themselves.
    BIBFRAME("http://bibframe.org/vocab/", "bf"),
    CONTENT("http://www.w3.org/2011/content#", "cnt"),
    DCTERMS("http://purl.org/dc/terms/", "dcterms"),    
    FOAF("http://xmlns.com/foaf/0.1/", "foaf"),
    LD4L("http://bib.ld4l.org/ontology/", "ld4l"),
    LEGACY("http://ld4l.org/ontology/bib/legacy/", "legacy"),
    LINGVO("http://www.lingvoj.org/ontology#", "lingvo"),
    MADSRDF("http://www.loc.gov/mads/rdf/v1#", "madsrdf"),
    OA("http://www.w3.org/ns/oa#", "oa"),
    OWL("http://www.w3.org/2002/07/owl#", "owl"),
    PROV("http://www.w3.org/ns/prov#", "prov"),
    RDF("http://www.w3.org/1999/02/22-rdf-syntax-ns#", "rdf"),
    RDFS("http://www.w3.org/2000/01/rdf-schema#", "rdfs"),
    RELATORS("http://id.loc.gov/vocabulary/relators/", "marcrel"), 
    SCHEMA("http://schema.org/", "schema"),
    SKOS("http://www.w3.org/2004/02/skos/core#", "skos"),
    TIME("http://www.w3.org/TR/owl-time/", "time"), 
    VIVO("http://vivoweb.org/ontology/core#", "vivo");

    private static final Logger LOGGER = 
            LogManager.getLogger(OntNamespace.class);
    
    private final String uri;
    private final String prefix;
    
    OntNamespace(String uri, String prefix) {
        this.uri = uri;
        this.prefix = prefix;
    }
    
    public String uri() {
        return this.uri;
    }
    
    public String prefix() {
        return this.prefix;
    }
    
    private static final Map<String, OntNamespace> LOOKUP_BY_URI = 
            new HashMap<String, OntNamespace>();

    private static final Map<String, String> LOOKUP_BY_PREFIX = 
            new HashMap<String, String>();
    
    static {
        for (OntNamespace ns : OntNamespace.values()) {
            LOOKUP_BY_URI.put(ns.uri, ns);
            LOOKUP_BY_PREFIX.put(ns.uri, ns.prefix);
        }
    }
    
    public static OntNamespace get(String uri) { 
        return LOOKUP_BY_URI.get(uri); 
    }
    
    public static String getNsPrefix(String uri) {
        return LOOKUP_BY_PREFIX.get(uri);
    }

}
