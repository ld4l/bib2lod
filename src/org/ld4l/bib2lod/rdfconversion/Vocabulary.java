package org.ld4l.bib2lod.rdfconversion;

public enum Vocabulary {

    FAST("http://id.worldcat.org/fast/", "fast"),
    LANGUAGE("http://id.loc.gov/vocabulary/languages/", "languages"),
    WORLDCAT("http://www.worldcat.org/oclc/", "worldcat"),
    
    // Invented vocabularies to create demonstration URIs. Should be replaced
    // with actual shelf mark vocabulary namespaces.
    DDC("http://id.loc.gov/ddc/", "ddc"),
    LCC("http://id.loc.gov/lcc/", "lcc"),
    NLM("http://id.loc.gov/nlm/", "nlm"),
    UDC("http://id.loc.gov/udc/", "udc");
    
    private final String uri;
    private final String label;
    
    Vocabulary(String uri, String label) {
        this.uri = uri;
        this.label = label;
    }
    
    Vocabulary(String uri) {
        this.uri = uri;
        this.label = null;
    }
    
    public String uri() {
        return uri;
    }
    
    public String label() {
        return label;
    }
    
}
